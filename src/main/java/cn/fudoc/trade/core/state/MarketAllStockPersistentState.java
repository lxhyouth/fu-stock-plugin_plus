package cn.fudoc.trade.core.state;

import cn.fudoc.trade.api.data.StockInfo;
import cn.fudoc.trade.core.state.index.MatchResult;
import cn.fudoc.trade.core.state.index.StockIndex;
import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@State(
        name = "fuAllStock",
        storages = @Storage("fu-all-stock.xml")
)
@Getter
@Setter
public class MarketAllStockPersistentState implements PersistentStateComponent<MarketAllStockPersistentState> {

    /**
     * 每次更新市场股票数据时间
     */
    private Long updateTime;

    /**
     * 大A股票索引
     */
    private StockIndex A;
    /**
     * 香港股票索引
     */
    private StockIndex HK;
    /**
     * 美国股票索引
     */
    private StockIndex US;

    public static MarketAllStockPersistentState getInstance() {
        return ApplicationManager.getApplication().getService(MarketAllStockPersistentState.class);
    }


    public List<StockInfo> match(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return new ArrayList<>();
        }
        List<MatchResult> matchList = Lists.newArrayList();

        if (Objects.nonNull(A)) {
            matchList.addAll(A.match(keyword));
        }
        if (Objects.nonNull(HK)) {
            matchList.addAll(HK.match(keyword));
        }
        if (Objects.nonNull(US)) {
            matchList.addAll(US.match(keyword));
        }
        //取匹配度最高的10条返回，并按股票代码去重
        return matchList.stream()
                .sorted(Comparator.comparing(MatchResult::getSimilarity).reversed())
                .collect(Collectors.toMap(
                        result -> result.getStockInfo().getStockCode(), // 以股票代码为key去重
                        result -> result,
                        (existing, replacement) -> existing // 保留第一个（相似度最高的）
                ))
                .values().stream()
                .limit(10)
                .map(MatchResult::getStockInfo)
                .collect(Collectors.toList());
    }


    @Override
    public @Nullable MarketAllStockPersistentState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull MarketAllStockPersistentState stockGroupPersistentState) {
        XmlSerializerUtil.copyBean(stockGroupPersistentState, this);
    }
}
