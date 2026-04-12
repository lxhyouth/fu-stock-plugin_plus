package cn.fudoc.trade.api;


import cn.fudoc.trade.api.data.RealStockInfo;
import cn.fudoc.trade.api.impl.ITickApiServiceImpl;
import cn.fudoc.trade.core.common.enumtype.JYSEnum;
import com.intellij.openapi.application.ApplicationManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 股票数据聚合服务
 * 智能选择最佳数据源获取实时行情
 * 
 * 策略：
 * - A股/港股: 优先使用腾讯API，备选新浪API
 * - 美股: 优先使用 iTick API，备选新浪API，最后腾讯API
 */
@Slf4j
public class StockDataAggregator {

    private final TencentApiService tencentApiService;
    private final SinaApiService sinaApiService;
    private final ITickApiService iTickApiService;

    public StockDataAggregator() {
        this.tencentApiService = ApplicationManager.getApplication().getService(TencentApiService.class);
        this.sinaApiService = ApplicationManager.getApplication().getService(SinaApiService.class);
        this.iTickApiService = new ITickApiServiceImpl();
    }

    /**
     * 获取股票实时数据（智能路由）
     *
     * @param codeSet 股票代码集合
     * @return 股票实时数据列表
     */
    public List<RealStockInfo> getStockData(Set<String> codeSet) {
        if (codeSet == null || codeSet.isEmpty()) {
            return Collections.emptyList();
        }

        // 按市场分组
        Map<JYSEnum, Set<String>> marketGroups = groupByMarket(codeSet);

        List<RealStockInfo> allResults = new ArrayList<>();

        // 处理每个市场
        for (Map.Entry<JYSEnum, Set<String>> entry : marketGroups.entrySet()) {
            JYSEnum market = entry.getKey();
            Set<String> codes = entry.getValue();

            List<RealStockInfo> results;
            if (market == JYSEnum.US) {
                // 美股：优先使用新浪API
                results = getUSStockData(codes);
            } else {
                // A股/港股：优先使用腾讯API
                results = getCNHKStockData(codes);
            }

            if (results != null && !results.isEmpty()) {
                allResults.addAll(results);
            }
        }

        return allResults;
    }

    /**
     * 按市场分组股票代码
     */
    private Map<JYSEnum, Set<String>> groupByMarket(Set<String> codeSet) {
        Map<JYSEnum, Set<String>> groups = new HashMap<>();

        for (String code : codeSet) {
            JYSEnum market = detectMarket(code);
            groups.computeIfAbsent(market, k -> new HashSet<>()).add(code);
        }

        return groups;
    }

    /**
     * 检测股票市场
     */
    private JYSEnum detectMarket(String code) {
        if (StringUtils.isBlank(code)) {
            return JYSEnum.DEFAULT;
        }

        if (code.startsWith(JYSEnum.SH.getCode())) {
            return JYSEnum.SH;
        } else if (code.startsWith(JYSEnum.SZ.getCode())) {
            return JYSEnum.SZ;
        } else if (code.startsWith(JYSEnum.HK.getCode())) {
            return JYSEnum.HK;
        } else if (code.startsWith(JYSEnum.US.getCode())) {
            return JYSEnum.US;
        }

        return JYSEnum.DEFAULT;
    }

    /**
     * 获取美股数据（iTick API 优先）
     */
    private List<RealStockInfo> getUSStockData(Set<String> codes) {
        // 第一优先级：iTick API
        try {
            System.out.println("[DEBUG Aggregator] Trying iTick API for US stocks: " + codes);
            List<RealStockInfo> results = iTickApiService.stockList(codes);
            
            if (results != null && !results.isEmpty()) {
                System.out.println("[DEBUG Aggregator] iTick API success: " + results.size() + " stocks");
                return results;
            }
            
            System.out.println("[DEBUG Aggregator] iTick API returned empty, trying Sina API");
        } catch (Exception e) {
            System.err.println("[ERROR Aggregator] iTick API failed: " + e.getMessage());
            log.warn("iTick API 获取美股数据失败，尝试新浪API", e);
        }
        
        // 第二优先级：新浪 API
        try {
            System.out.println("[DEBUG Aggregator] Trying Sina API for US stocks: " + codes);
            List<RealStockInfo> results = sinaApiService.stockList(codes);
            
            if (results != null && !results.isEmpty()) {
                System.out.println("[DEBUG Aggregator] Sina API success: " + results.size() + " stocks");
                return results;
            }
            
            System.out.println("[DEBUG Aggregator] Sina API returned empty, trying Tencent API");
        } catch (Exception e) {
            System.err.println("[ERROR Aggregator] Sina API failed: " + e.getMessage());
            log.warn("新浪API 获取美股数据失败，尝试腾讯API", e);
        }
        
        // 第三优先级：腾讯 API（备用）
        try {
            System.out.println("[DEBUG Aggregator] Trying Tencent API for US stocks: " + codes);
            return tencentApiService.stockList(codes);
        } catch (Exception e) {
            System.err.println("[ERROR Aggregator] Tencent API failed: " + e.getMessage());
            log.error("所有 API 获取美股数据均失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取A股/港股数据（腾讯API优先）
     */
    private List<RealStockInfo> getCNHKStockData(Set<String> codes) {
        try {
            log.debug("使用腾讯API获取A股/港股数据: {}", codes);
            List<RealStockInfo> results = tencentApiService.stockList(codes);
            
            if (results != null && !results.isEmpty()) {
                log.debug("腾讯API成功获取{}条A股/港股数据", results.size());
                return results;
            }
            
            // 如果腾讯API失败，尝试新浪API
            log.warn("腾讯API获取A股/港股数据为空，尝试新浪API");
            return sinaApiService.stockList(codes);
        } catch (Exception e) {
            log.error("获取A股/港股数据失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 测试API连接性
     */
    public Map<String, Boolean> testApiConnectivity() {
        Map<String, Boolean> results = new HashMap<>();
        
        // 测试腾讯API
        try {
            tencentApiService.ping();
            results.put("Tencent API", true);
        } catch (Exception e) {
            results.put("Tencent API", false);
        }
        
        // 测试新浪API
        try {
            sinaApiService.ping();
            results.put("Sina API", true);
        } catch (Exception e) {
            results.put("Sina API", false);
        }
        
        return results;
    }
}
