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
        
        // 转换股票代码为新浪格式
        List<String> sinaCodes = new ArrayList<>();
        Map<String, String> codeMapping = new HashMap<>(); // 记录原始代码和新浪代码的映射
        
        for (String code : codeSet) {
            String sinaCode = convertToSinaCode(code);
            if (sinaCode != null) {
                sinaCodes.add(sinaCode);
                codeMapping.put(sinaCode, code);
            }
        }
        
        if (sinaCodes.isEmpty()) {
            return new ArrayList<>();
        }
        
        String codeStr = String.join(",", sinaCodes);
        String requestUrl = BASE_URL + codeStr;
        
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
            
            return parseStockSegment(result, codeMapping);
        } catch (Exception e) {
            log.warn("从新浪财经获取股票实时信息异常:{}", e.getMessage());
        }
        
        return new ArrayList<>();
    }

    /**
     * 将内部股票代码转换为新浪格式
     * usAAPL -> gb_aapl
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
        List<RealStockInfo> realStockInfoList = new ArrayList<>();
        
        if (StringUtils.isBlank(result)) {
            return realStockInfoList;
        }
        
        String[] lines = result.split("\n");
        for (String line : lines) {
            if (StringUtils.isBlank(line) || !line.contains("=")) {
                continue;
            }
            
            try {
                // 提取股票代码和数据
                String codePart = line.substring(0, line.indexOf("="));
                String dataPart = line.substring(line.indexOf("=") + 2, line.length() - 1);
                
                // 提取新浪代码
                String sinaCode = codePart.substring(codePart.lastIndexOf("_") + 1);
                
                // 转换为原始代码
                String originalCode = codeMapping.getOrDefault(sinaCode, sinaCode);
                
                // 解析数据
                String[] values = dataPart.split(",");
                if (values.length < 4) {
                    continue;
                }
                
                RealStockInfo bean = new RealStockInfo();
                bean.setStockCode(originalCode);
                
                // 判断是美股还是A股/港股
                if (sinaCode.startsWith("gb_")) {
                    // 美股数据格式
                    parseUSStockData(bean, values);
                } else {
                    // A股/港股数据格式
                    parseCNHKStockData(bean, values);
                }
                
                realStockInfoList.add(bean);
            } catch (Exception e) {
                log.warn("解析股票数据失败: {}", line, e);
            }
        }
        
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
     * values[0]: 股票名称
     * values[1]: 当前价格
     * values[2]: 涨跌额
     * values[3]: 涨跌幅%
     * values[4]: 成交量
     * values[5]: 最新成交时间
     * values[6]: 开盘价
     * values[7]: 最高价
     * values[8]: 最低价
     * values[9]: 昨日收盘价
     */
    private static void parseUSStockData(RealStockInfo bean, String[] values) {
        bean.setStockName(values[0]);
        bean.setCurrentPrice(values[1]);
        bean.setYesterdayPrice(values.length > 9 ? values[9] : "");
        
        // 涨跌幅
        if (values.length > 3 && StringUtils.isNotBlank(values[3])) {
            bean.setIncreaseRate(values[3]);
        }
        
        // 成交量/额
        if (values.length > 4) {
            String[] volume = formatVolume(values[4]);
            bean.setVolume(volume[0]);
            bean.setVolumeUnit(volume[1]);
        } else {
            bean.setVolume("---");
            bean.setVolumeUnit("");
        }
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
                    .divide(yesterday, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
            
            return FuNumberUtil.format(increase.setScale(2, BigDecimal.ROUND_HALF_UP));
        } catch (Exception e) {
            return "0";
        }
    }

    /**
     * 格式化成交量/额
     */
    private static String[] formatVolume(String volume) {
        if (StringUtils.isEmpty(volume) || !NumberUtil.isNumber(volume)) {
            return new String[]{"---", ""};
        }
        
        try {
            BigDecimal bigDecimal = new BigDecimal(volume);
            if (bigDecimal.compareTo(Y) < 0) {
                return new String[]{volume, "万"};
            }
            return new String[]{FuNumberUtil.format(NumberUtil.div(bigDecimal, Y, 2)), "亿"};
        } catch (Exception e) {
            return new String[]{"---", ""};
        }
    }
}
