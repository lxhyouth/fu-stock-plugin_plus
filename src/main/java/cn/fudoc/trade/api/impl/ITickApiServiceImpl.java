package cn.fudoc.trade.api.impl;

import cn.fudoc.trade.api.ITickApiService;
import cn.fudoc.trade.api.data.RealStockInfo;
import cn.fudoc.trade.core.state.FuStockSettingState;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.intellij.openapi.components.Service;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * iTick API 服务实现
 * 提供美股实时行情数据
 * 
 * API 文档: https://itick.io
 * 免费套餐: 无限调用基本行情
 */
@Slf4j
@Service
public class ITickApiServiceImpl implements ITickApiService {
    
    private static final String BASE_URL = "https://api.itick.org/stock/quote";
    
    /**
     * 从配置中获取 iTick API Token
     */
    private String getApiToken() {
        try {
            FuStockSettingState settingState = FuStockSettingState.getInstance();
            String token = settingState.getITickApiToken();
            if (StringUtils.isNotBlank(token)) {
                return token;
            }
        } catch (Exception e) {
            log.warn("获取 iTick API Token 失败: {}", e.getMessage());
        }
        return null;
    }
    
    @Override
    public List<RealStockInfo> stockList(Set<String> codeSet) {
        if (codeSet == null || codeSet.isEmpty()) {
            return new ArrayList<>();
        }
        
        System.out.println("[DEBUG iTick] stockList called with codes: " + codeSet);
        
        List<RealStockInfo> result = new ArrayList<>();
        
        // iTick API 每次只能查询一只股票，需要逐个请求
        for (String code : codeSet) {
            try {
                RealStockInfo stockInfo = fetchSingleStock(code);
                if (stockInfo != null) {
                    result.add(stockInfo);
                }
            } catch (Exception e) {
                log.warn("iTick API 获取股票 {} 数据失败: {}", code, e.getMessage());
                System.err.println("[ERROR iTick] Failed to fetch stock " + code + ": " + e.getMessage());
            }
        }
        
        System.out.println("[DEBUG iTick] Successfully fetched " + result.size() + " stocks");
        return result;
    }
    
    /**
     * 获取单只股票的实时行情
     */
    private RealStockInfo fetchSingleStock(String code) {
        // 转换股票代码：usAAPL -> AAPL
        String cleanCode = code.startsWith("us") ? code.substring(2) : code;
        
        System.out.println("[DEBUG iTick] Fetching stock: " + cleanCode);
        
        // 获取 API Token
        String apiToken = getApiToken();
        if (StringUtils.isBlank(apiToken)) {
            System.out.println("[WARN iTick] API Token not configured, skipping iTick API");
            return null;
        }
        
        try {
            // 构建请求 URL
            String url = BASE_URL + "?region=US&code=" + cleanCode;
            
            // 设置请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");
            headers.put("token", apiToken);
            
            // 发送请求
            String response = HttpUtil.createGet(url)
                    .addHeaders(headers)
                    .timeout(5000)
                    .execute()
                    .body();
            
            System.out.println("[DEBUG iTick] Response: " + response);
            
            // 解析响应
            return parseResponse(response, code);
            
        } catch (Exception e) {
            log.error("iTick API 请求失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 解析 iTick API 响应
     * 
     * 响应格式示例：
     * {
     *   "code": 0,
     *   "msg": null,
     *   "data": {
     *     "s": "IQ",              // symbol (股票代码)
     *     "ld": 1.25,             // last deal (当前价格)
     *     "p": 1.28,              // previous close (昨日收盘价)
     *     "o": 1.29,              // open (开盘价)
     *     "h": 1.3,               // high (最高价)
     *     "l": 1.25,              // low (最低价)
     *     "t": 1775855141519,     // timestamp (时间戳)
     *     "v": 6879385,           // volume (成交量)
     *     "tu": 8699313.941,      // turnover (成交额)
     *     "ch": -0.03,            // change (涨跌额)
     *     "chp": -2.3400          // change percent (涨跌幅%)
     *   }
     * }
     */
    private RealStockInfo parseResponse(String response, String originalCode) {
        if (StringUtils.isBlank(response)) {
            return null;
        }
        
        try {
            JSONObject jsonObject = JSONUtil.parseObj(response);
            
            // 检查响应状态（处理错误响应）
            Integer code = jsonObject.getInt("code");
            if (code == null || code != 0) {
                String msg = jsonObject.getStr("msg", jsonObject.getStr("message", "Unknown error"));
                log.warn("iTick API 返回错误: {}", msg);
                System.err.println("[ERROR iTick] API error: " + msg);
                return null;
            }
            
            JSONObject data = jsonObject.getJSONObject("data");
            if (data == null) {
                System.err.println("[ERROR iTick] No data in response");
                return null;
            }
            
            RealStockInfo stockInfo = new RealStockInfo();
            stockInfo.setStockCode(originalCode);
            
            // 股票名称（iTick API 不返回名称，需要后续补充）
            stockInfo.setStockName("");
            
            // 当前价格 (ld = last deal)
            Double lastPrice = data.getDouble("ld");
            if (lastPrice != null) {
                stockInfo.setCurrentPrice(String.valueOf(lastPrice));
            } else {
                System.err.println("[WARN iTick] Current price is null for " + originalCode);
            }
            
            // 昨日收盘价 (p = previous close)
            Double prevClose = data.getDouble("p");
            if (prevClose != null) {
                stockInfo.setYesterdayPrice(String.valueOf(prevClose));
            }
            
            // 涨跌幅% (chp = change percent)
            Double percentChange = data.getDouble("chp");
            if (percentChange != null) {
                stockInfo.setIncreaseRate(formatNumber(percentChange));
            }
            
            // 成交额 (tu = turnover)
            Double amount = data.getDouble("tu");
            if (amount != null && amount > 0) {
                String[] formatted = formatVolume(amount.toString());
                stockInfo.setVolume(formatted[0]);
                stockInfo.setVolumeUnit(formatted[1]);
                System.out.println("[DEBUG iTick] Formatted amount: " + formatted[0] + formatted[1]);
            } else {
                stockInfo.setVolume("---");
                stockInfo.setVolumeUnit("");
            }
            
            System.out.println("[DEBUG iTick] Parsed stock: " + stockInfo.getStockName() + 
                ", price: " + stockInfo.getCurrentPrice() + 
                ", change: " + stockInfo.getIncreaseRate() + "%");
            
            return stockInfo;
            
        } catch (Exception e) {
            log.error("解析 iTick API 响应失败: {}", e.getMessage(), e);
            System.err.println("[ERROR iTick] Parse exception: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 格式化数字（保留2位小数）
     */
    private String formatNumber(Double value) {
        if (value == null) {
            return "0";
        }
        BigDecimal bd = new BigDecimal(value);
        return bd.setScale(2, RoundingMode.HALF_UP).toString();
    }
    
    /**
     * 格式化成交量/额
     */
    private String[] formatVolume(String volume) {
        if (StringUtils.isEmpty(volume) || !NumberUtil.isNumber(volume)) {
            return new String[]{"---", ""};
        }
        
        try {
            BigDecimal bigDecimal = new BigDecimal(volume);
            BigDecimal hundredMillion = new BigDecimal("100000000"); // 亿
            BigDecimal tenThousand = new BigDecimal("10000");         // 万
            
            if (bigDecimal.compareTo(hundredMillion) >= 0) {
                // >= 1亿，显示为"亿"
                BigDecimal result = NumberUtil.div(bigDecimal, hundredMillion, 2);
                return new String[]{formatNumber(result.doubleValue()), "亿"};
            } else if (bigDecimal.compareTo(tenThousand) >= 0) {
                // >= 1万，显示为"万"
                BigDecimal result = NumberUtil.div(bigDecimal, tenThousand, 2);
                return new String[]{formatNumber(result.doubleValue()), "万"};
            } else {
                // < 1万，直接显示
                return new String[]{formatNumber(bigDecimal.doubleValue()), ""};
            }
        } catch (Exception e) {
            return new String[]{"---", ""};
        }
    }
}
