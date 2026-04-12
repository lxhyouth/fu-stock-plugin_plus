package cn.fudoc.trade.api.impl;

import cn.fudoc.trade.api.SinaApiService;
import cn.fudoc.trade.api.data.RealStockInfo;
import cn.fudoc.trade.core.common.FuNotification;
import cn.fudoc.trade.util.FuNumberUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.http.HttpUtil;
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
 * 新浪财经API数据获取实现类
 * 支持A股、港股、美股实时行情
 * 
 * API地址: http://hq.sinajs.cn/list={codes}
 * 美股代码格式: gb_{symbol} (如 gb_aapl, gb_tsla)
 */
@Slf4j
public class SinaApiServiceImpl implements SinaApiService {
    
    private static final BigDecimal Y = new BigDecimal("10000");
    private static final String BASE_URL = "http://hq.sinajs.cn/list=";
    
    /**
     * 美股代码映射表（常用美股）
     * key: us前缀的代码 (如 usAAPL)
     * value: 新浪格式 (如 gb_aapl)
     */
    private static final Map<String, String> US_STOCK_CODE_MAP = new HashMap<>();
    
    static {
        // 初始化常用美股代码映射
        // 科技股
        US_STOCK_CODE_MAP.put("usAAPL", "gb_aapl");      // 苹果
        US_STOCK_CODE_MAP.put("usMSFT", "gb_msft");      // 微软
        US_STOCK_CODE_MAP.put("usGOOGL", "gb_googl");    // 谷歌
        US_STOCK_CODE_MAP.put("usAMZN", "gb_amzn");      // 亚马逊
        US_STOCK_CODE_MAP.put("usTSLA", "gb_tsla");      // 特斯拉
        US_STOCK_CODE_MAP.put("usMETA", "gb_meta");      // Meta
        US_STOCK_CODE_MAP.put("usNVDA", "gb_nvda");      // 英伟达
        US_STOCK_CODE_MAP.put("usNFLX", "gb_nflx");      // Netflix
        
        // 金融股
        US_STOCK_CODE_MAP.put("usJPM", "gb_jpm");        // 摩根大通
        US_STOCK_CODE_MAP.put("usBAC", "gb_bac");        // 美国银行
        US_STOCK_CODE_MAP.put("usGS", "gb_gs");          // 高盛
        
        // 消费股
        US_STOCK_CODE_MAP.put("usKO", "gb_ko");          // 可口可乐
        US_STOCK_CODE_MAP.put("usPEP", "gb_pep");        // 百事可乐
        US_STOCK_CODE_MAP.put("usMCD", "gb_mcd");        // 麦当劳
        US_STOCK_CODE_MAP.put("usNKE", "gb_nke");        // 耐克
        
        // 医药股
        US_STOCK_CODE_MAP.put("usJNJ", "gb_jnj");        // 强生
        US_STOCK_CODE_MAP.put("usPFE", "gb_pfe");        // 辉瑞
        US_STOCK_CODE_MAP.put("usMRNA", "gb_mrna");      // Moderna

        // 影视
        US_STOCK_CODE_MAP.put("usIQ", "gb_iq");           // 爱奇艺

    }

    @Override
    public void ping() {
        String url = BASE_URL + "sh600519";
        try {
            HttpUtil.get(url);
        } catch (Exception e) {
            FuNotification.notifyWarning("无法获取股票信息，可能您当前环境无法访问api：" + url);
        }
    }

    @Override
    public List<RealStockInfo> stockList(Set<String> codeSet) {
        if (codeSet == null || codeSet.isEmpty()) {
            return new ArrayList<>();
        }
        
        System.out.println("[DEBUG Sina API] stockList called with codes: " + codeSet);
        
        // 转换股票代码为新浪格式
        List<String> sinaCodes = new ArrayList<>();
        Map<String, String> codeMapping = new HashMap<>(); // 记录原始代码和新浪代码的映射
        
        for (String code : codeSet) {
            String sinaCode = convertToSinaCode(code);
            System.out.println("[DEBUG Sina API] Converting " + code + " -> " + sinaCode);
            if (sinaCode != null) {
                sinaCodes.add(sinaCode);
                codeMapping.put(sinaCode, code);
            }
        }
        
        if (sinaCodes.isEmpty()) {
            System.out.println("[DEBUG Sina API] No valid sina codes, returning empty list");
            return new ArrayList<>();
        }
        
        String codeStr = String.join(",", sinaCodes);
        String requestUrl = BASE_URL + codeStr;
        System.out.println("[DEBUG Sina API] Request URL: " + requestUrl);
        
        try {
            // 设置请求头，避免被拒绝访问
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://finance.sina.com.cn");
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            String result = HttpUtil.createGet(requestUrl)
                    .addHeaders(headers)
                    .timeout(5000)
                    .execute()
                    .body();
            
            System.out.println("[DEBUG Sina API] Response length: " + (result != null ? result.length() : 0));
            if (result != null && result.length() < 500) {
                System.out.println("[DEBUG Sina API] Response preview: " + result);
            }
            
            return parseStockSegment(result, codeMapping);
        } catch (Exception e) {
            log.warn("从新浪财经获取股票实时信息异常:{}", e.getMessage());
        }
        
        return new ArrayList<>();
    }

    /**
     * 将内部股票代码转换为新浪格式
     * usAAPL -> gb_aapl
     * AAPL -> gb_aapl (兼容不带前缀的情况)
     * sh600519 -> sh600519
     * hk00700 -> hk00700
     */
    private String convertToSinaCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        
        // 美股代码转换
        if (code.startsWith("us")) {
            String upperCode = code.toUpperCase();
            // 先查映射表
            if (US_STOCK_CODE_MAP.containsKey(upperCode)) {
                return US_STOCK_CODE_MAP.get(upperCode);
            }
            // 如果不在映射表中，尝试通用转换 usXXX -> gb_xxx
            String symbol = code.substring(2).toLowerCase();
            return "gb_" + symbol;
        }
        
        // 兼容不带前缀的美股代码（如 nvda, iq, NVDA, IQ）
        // 检查是否是纯字母组成的代码（美股特征）
        if (code.matches("^[a-zA-Z]+$")) {
            String upperCode = "us" + code.toUpperCase();
            if (US_STOCK_CODE_MAP.containsKey(upperCode)) {
                return US_STOCK_CODE_MAP.get(upperCode);
            }
            // 通用转换
            return "gb_" + code.toLowerCase();
        }
        
        // A股和港股直接使用
        return code;
    }

    /**
     * 解析新浪股票数据
     * 
     * A股/港股格式:
     * var hq_str_sh600519="贵州茅台,27.55,27.25,26.91,27.55,26.20,26.91,26.92,..."
     * 
     * 美股格式:
     * var hq_str_gb_aapl="Apple Inc.,150.00,149.50,151.00,148.50,150.50,1000000,..."
     */
    private static List<RealStockInfo> parseStockSegment(String result, Map<String, String> codeMapping) {
        System.out.println("[DEBUG Parse] ===== Start parseStockSegment =====");
        System.out.println("[DEBUG Parse] Result length: " + (result != null ? result.length() : 0));
        System.out.println("[DEBUG Parse] Code mapping size: " + codeMapping.size());
        
        List<RealStockInfo> realStockInfoList = new ArrayList<>();
        
        if (StringUtils.isBlank(result)) {
            System.out.println("[DEBUG Parse] Result is blank");
            return realStockInfoList;
        }
        
        String[] lines = result.split("\n");
        System.out.println("[DEBUG Parse] Lines count: " + lines.length);
        
        for (String line : lines) {
            if (StringUtils.isBlank(line) || !line.contains("=")) {
                continue;
            }
            
            try {
                System.out.println("[DEBUG Parse] Processing line: " + line.substring(0, Math.min(50, line.length())));
                
                // 提取股票代码和数据
                String codePart = line.substring(0, line.indexOf("="));
                String dataPart = line.substring(line.indexOf("=") + 2, line.length() - 1);
                
                System.out.println("[DEBUG Parse] codePart: " + codePart);
                System.out.println("[DEBUG Parse] dataPart length: " + dataPart.length());
                
                // 提取新浪代码：var hq_str_gb_iq -> gb_iq
                // 格式固定为：var hq_str_XXX，需要提取 hq_str_ 后面的部分
                String prefix = "hq_str_";
                int prefixIndex = codePart.indexOf(prefix);
                if (prefixIndex == -1) {
                    System.err.println("[ERROR Parse] Invalid code format: " + codePart);
                    continue;
                }
                String sinaCode = codePart.substring(prefixIndex + prefix.length());
                System.out.println("[DEBUG Parse] sinaCode: " + sinaCode);
                
                // 转换为原始代码
                String originalCode = codeMapping.getOrDefault(sinaCode, sinaCode);
                System.out.println("[DEBUG Parse] originalCode: " + originalCode);
                
                // 解析数据
                String[] values = dataPart.split(",");
                System.out.println("[DEBUG Parse] values length: " + values.length);
                
                if (values.length < 4) {
                    System.out.println("[DEBUG Parse] values length < 4, skip");
                    continue;
                }
                
                RealStockInfo bean = new RealStockInfo();
                bean.setStockCode(originalCode);
                
                // 判断是美股还是A股/港股
                if (sinaCode.startsWith("gb_")) {
                    System.out.println("[DEBUG Parse] Detected US stock (gb_)");
                    // 美股数据格式
                    parseUSStockData(bean, values);
                } else {
                    System.out.println("[DEBUG Parse] Detected CN/HK stock");
                    // A股/港股数据格式
                    parseCNHKStockData(bean, values);
                }
                
                realStockInfoList.add(bean);
                System.out.println("[DEBUG Parse] Added stock: " + bean.getStockName());
            } catch (Exception e) {
                System.err.println("[ERROR Parse] Exception: " + e.getMessage());
                e.printStackTrace();
                log.warn("解析股票数据失败: {}", line, e);
            }
        }
        
        System.out.println("[DEBUG Parse] Total stocks parsed: " + realStockInfoList.size());
        System.out.println("[DEBUG Parse] ===== End parseStockSegment =====");
        return realStockInfoList;
    }

    /**
     * 解析A股/港股数据
     * values[0]: 股票名称
     * values[1]: 今日开盘价
     * values[2]: 昨日收盘价
     * values[3]: 当前价格
     * values[4]: 今日最高价
     * values[5]: 今日最低价
     * values[30]: 日期
     * values[31]: 时间
     * values[32]: 涨跌额
     * values[33]: 涨跌幅%
     */
    private static void parseCNHKStockData(RealStockInfo bean, String[] values) {
        bean.setStockName(values[0]);
        bean.setCurrentPrice(values[3]);
        bean.setYesterdayPrice(values[2]);
        
        // 计算涨跌幅
        if (values.length > 33 && StringUtils.isNotBlank(values[33])) {
            bean.setIncreaseRate(values[33]);
        } else {
            // 手动计算涨跌幅
            bean.setIncreaseRate(calculateIncreaseRate(values[3], values[2]));
        }
        
        // 成交额（values[37]）
        if (values.length > 37) {
            String[] volume = formatVolume(values[37]);
            bean.setVolume(volume[0]);
            bean.setVolumeUnit(volume[1]);
        } else {
            bean.setVolume("---");
            bean.setVolumeUnit("");
        }
    }

    /**
     * 解析美股数据
     * 根据实际API返回数据分析：
     * values[0]: 股票名称 (爱奇艺)
     * values[1]: 当前价格 (1.2500)
     * values[2]: 涨跌幅% (-2.34)
     * values[3]: 时间 (2026-04-11 06:44:39)
     * values[4]: 涨跌额 (-0.0300)
     * values[5]: 开盘价 (1.2900)
     * values[6]: 最高价 (1.3000)
     * values[7]: 最低价 (1.2500)
     * values[8]: 52周最高 (2.8400)
     * values[9]: 52周最低 (1.1800)
     * values[10]: 成交量-手 (6894075)
     * values[11]: 成交量-股 (9949621)
     * values[12]: 未知字段
     * ...
     * values[30]: 成交额 (美元) - 所有美股统一使用此字段
     */
    private static void parseUSStockData(RealStockInfo bean, String[] values) {
        System.out.println("[DEBUG Sina US] ===== Parsing US Stock Data =====");
        System.out.println("[DEBUG Sina US] Stock name: " + values[0]);
        System.out.println("[DEBUG Sina US] Total values count: " + values.length);
        System.out.println("[DEBUG Sina US] Current price (values[1]): " + values[1]);
        System.out.println("[DEBUG Sina US] Increase rate% (values[2]): " + values[2]);
        
        // 打印所有字段及其索引
        StringBuilder allFields = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            allFields.append("[").append(i).append("]=").append(values[i]).append(" | ");
        }
        System.out.println("[DEBUG Sina US] All fields: " + allFields.toString());
        
        bean.setStockName(values[0]);
        bean.setCurrentPrice(values[1]);
        
        // 最高价 (values[6])
        if (values.length > 6 && StringUtils.isNotBlank(values[6])) {
            bean.setHighPrice(values[6]);
        }
        
        // 最低价 (values[7])
        if (values.length > 7 && StringUtils.isNotBlank(values[7])) {
            bean.setLowPrice(values[7]);
        }
        
        // 涨跌幅 (values[2])
        if (values.length > 2 && StringUtils.isNotBlank(values[2])) {
            bean.setIncreaseRate(values[2]);
        }
        
        // 昨日收盘价 = 当前价格 - 涨跌额
        if (values.length > 4 && StringUtils.isNotBlank(values[4]) && StringUtils.isNotBlank(values[1])) {
            try {
                BigDecimal currentPrice = new BigDecimal(values[1]);
                BigDecimal change = new BigDecimal(values[4]);
                BigDecimal yesterdayPrice = currentPrice.subtract(change);
                bean.setYesterdayPrice(yesterdayPrice.toString());
            } catch (Exception e) {
                bean.setYesterdayPrice("");
            }
        }
        
        // 成交额：统一使用 values[30]
        if (values.length > 30 && StringUtils.isNotBlank(values[30])) {
            try {
                System.out.println("[DEBUG Sina US] Using values[30] for amount: " + values[30]);
                String[] formattedAmount = formatVolume(values[30]);
                bean.setVolume(formattedAmount[0]);
                bean.setVolumeUnit(formattedAmount[1]);
                System.out.println("[DEBUG Sina US] Formatted amount: " + formattedAmount[0] + formattedAmount[1]);
            } catch (Exception e) {
                System.err.println("[ERROR Sina US] Failed to format amount from values[30]: " + e.getMessage());
                log.warn("格式化美股成交额失败: {}", e.getMessage());
                bean.setVolume("---");
                bean.setVolumeUnit("");
            }
        } else {
            System.out.println("[DEBUG Sina US] values[30] not available, length=" + values.length);
            bean.setVolume("---");
            bean.setVolumeUnit("");
        }
        
        System.out.println("[DEBUG Sina US] ===== End Parsing =====");
    }

    /**
     * 计算涨跌幅
     */
    private static String calculateIncreaseRate(String currentPrice, String yesterdayPrice) {
        if (StringUtils.isBlank(currentPrice) || StringUtils.isBlank(yesterdayPrice)) {
            return "0";
        }
        
        try {
            BigDecimal current = new BigDecimal(currentPrice);
            BigDecimal yesterday = new BigDecimal(yesterdayPrice);
            
            if (yesterday.compareTo(BigDecimal.ZERO) == 0) {
                return "0";
            }
            
            BigDecimal increase = current.subtract(yesterday)
                    .divide(yesterday, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            
            return FuNumberUtil.format(increase.setScale(2, RoundingMode.HALF_UP));
        } catch (Exception e) {
            return "0";
        }
    }

    /**
     * 格式化成交量/额
     * 所有单位都保留两位小数
     */
    private static String[] formatVolume(String volume) {
        if (StringUtils.isEmpty(volume) || !NumberUtil.isNumber(volume)) {
            return new String[]{"---", ""};
        }
        
        try {
            BigDecimal bigDecimal = new BigDecimal(volume);
            BigDecimal hundredMillion = new BigDecimal("100000000"); // 亿
            BigDecimal tenThousand = new BigDecimal("10000");         // 万
            
            if (bigDecimal.compareTo(hundredMillion) >= 0) {
                // >= 1亿，显示为“亿”，保疙2位小数
                return new String[]{FuNumberUtil.format(NumberUtil.div(bigDecimal, hundredMillion, 2)), "亿"};
            } else if (bigDecimal.compareTo(tenThousand) >= 0) {
                // >= 1万，显示为“万”，保疙2位小数
                return new String[]{FuNumberUtil.format(NumberUtil.div(bigDecimal, tenThousand, 2)), "万"};
            } else {
                // < 1万，直接显示，保疙2位小数
                return new String[]{FuNumberUtil.format(bigDecimal.setScale(2, RoundingMode.HALF_UP)), ""};
            }
        } catch (Exception e) {
            return new String[]{"---", ""};
        }
    }
}
