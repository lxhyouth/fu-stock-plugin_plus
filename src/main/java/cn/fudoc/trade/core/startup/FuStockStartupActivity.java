package cn.fudoc.trade.core.startup;

import cn.fudoc.trade.api.ZTApiService;
import cn.fudoc.trade.core.common.enumtype.CNMappingGroupEnum;
import cn.fudoc.trade.core.common.enumtype.GroupTypeEnum;
import cn.fudoc.trade.core.state.HoldingsStockState;
import cn.fudoc.trade.core.state.MarketAllStockPersistentState;
import cn.fudoc.trade.core.state.StockGroupState;
import cn.fudoc.trade.core.state.index.StockIndex;
import cn.fudoc.trade.core.state.pojo.StockGroupInfo;
import cn.fudoc.trade.util.PinyinUtil;
import cn.fudoc.trade.view.helper.HideTextHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * IDE 启动后执行的初始化逻辑（注册各类监听器）
 */
public class FuStockStartupActivity implements ProjectActivity {
    // 标记：确保监听器只注册一次（避免重复注册）
    private static boolean isInitialized = false;
    private static final Long ONE_DAY = 24 * 60 * 60 * 1000L;


    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 1. 仅初始化一次（避免多项目场景下重复注册）
        if (isInitialized) return null;
        isInitialized = true;
        
        // 2. 使用 IDEA 的线程池执行异步任务，避免 SecurityException
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                MarketAllStockPersistentState instance = MarketAllStockPersistentState.getInstance();
                Long updateTime = instance.getUpdateTime();
                if (Objects.isNull(updateTime) || (updateTime + ONE_DAY) < System.currentTimeMillis()) {
                    //触发更新
                    ZTApiService ztApiService = ApplicationManager.getApplication().getService(ZTApiService.class);
                    
                    System.out.println("[DEBUG] Loading A stocks...");
                    instance.setA(new StockIndex(ztApiService.marketA(), false, false));
                    
                    System.out.println("[DEBUG] Loading HK stocks...");
                    instance.setHK(new StockIndex(ztApiService.marketHK(), true, false));
                    
                    System.out.println("[DEBUG] Loading US stocks...");
                    instance.setUS(new StockIndex(ztApiService.marketUS(), false, true));
                    
                    instance.setUpdateTime(System.currentTimeMillis());
                    System.out.println("[DEBUG] Stock data loaded successfully");
                }

                //处理历史数据问题
                StockGroupState stockGroupState = StockGroupState.getInstance();
                HoldingsStockState holdingsState = HoldingsStockState.getInstance();
                Map<String, GroupTypeEnum> stockTabEnumMap = stockGroupState.getStockTabEnumMap();
                if (stockTabEnumMap.isEmpty()) {
                    return;
                }
                stockTabEnumMap.forEach((key, value) -> {
                    GroupTypeEnum groupTypeEnum = holdingsState.getHoldings().containsKey(key) ? GroupTypeEnum.STOCK_HOLD : GroupTypeEnum.STOCK_INFO;
                    stockGroupState.add(new StockGroupInfo(key, HideTextHelper.mapping(key, CNMappingGroupEnum.STOCK_GROUP), groupTypeEnum));
                });
                stockTabEnumMap.clear();
                stockGroupState.setStockTabEnumMap(stockTabEnumMap);
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to load stock data: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        return null;
    }
}