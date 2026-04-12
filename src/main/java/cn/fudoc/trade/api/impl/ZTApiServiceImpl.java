package cn.fudoc.trade.api.impl;

import cn.fudoc.trade.api.ZTApiService;
import cn.fudoc.trade.api.data.StockInfo;
import cn.fudoc.trade.api.dto.ZTStockDTO;
import cn.fudoc.trade.api.helper.ZTTokenHelper;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 智兔数服金融API
 */
public class ZTApiServiceImpl implements ZTApiService {

    private static final String marketAUrl = "https://api.zhituapi.com/hs/list/all?token=";
    private static final String marketHKUrl = "https://api.zhituapi.com/hk/list/all?token=";
    private static final String marketUSUrl = "https://api.zhituapi.com/us/list/all?token=";


    @Override
    public List<StockInfo> marketA() {
        return getStockListByMarket(marketAUrl);
    }

    @Override
    public List<StockInfo> marketHK() {
        return getStockListByMarket(marketHKUrl);
    }

    @Override
    public List<StockInfo> marketUS() {
        // 智兔API不支持美股列表，返回常用的美股代码列表
        return getCommonUSStocks();
    }

    /**
     * 获取常用美股列表（因为智兔API不支持美股）
     */
    private List<StockInfo> getCommonUSStocks() {
        List<StockInfo> stocks = new java.util.ArrayList<>();
        
        // 科技股
        addUSStock(stocks, "AAPL", "Apple Inc.");
        addUSStock(stocks, "MSFT", "Microsoft Corp");
        addUSStock(stocks, "GOOGL", "Alphabet Inc Class A");
        addUSStock(stocks, "AMZN", "Amazon.com Inc");
        addUSStock(stocks, "TSLA", "Tesla Inc");
        addUSStock(stocks, "META", "Meta Platforms Inc");
        addUSStock(stocks, "NVDA", "NVIDIA Corp");
        addUSStock(stocks, "NFLX", "Netflix Inc");
        addUSStock(stocks, "AMD", "Advanced Micro Devices");
        addUSStock(stocks, "INTC", "Intel Corp");
        addUSStock(stocks, "CRM", "Salesforce Inc");
        addUSStock(stocks, "ORCL", "Oracle Corp");
        
        // 金融股
        addUSStock(stocks, "JPM", "JPMorgan Chase & Co");
        addUSStock(stocks, "BAC", "Bank of America Corp");
        addUSStock(stocks, "GS", "Goldman Sachs Group");
        addUSStock(stocks, "MS", "Morgan Stanley");
        addUSStock(stocks, "V", "Visa Inc");
        addUSStock(stocks, "MA", "Mastercard Inc");
        
        // 消费股
        addUSStock(stocks, "KO", "Coca-Cola Co");
        addUSStock(stocks, "PEP", "PepsiCo Inc");
        addUSStock(stocks, "MCD", "McDonald's Corp");
        addUSStock(stocks, "NKE", "Nike Inc");
        addUSStock(stocks, "SBUX", "Starbucks Corp");
        addUSStock(stocks, "WMT", "Walmart Inc");
        
        // 医药股
        addUSStock(stocks, "JNJ", "Johnson & Johnson");
        addUSStock(stocks, "PFE", "Pfizer Inc");
        addUSStock(stocks, "MRNA", "Moderna Inc");
        addUSStock(stocks, "UNH", "UnitedHealth Group");
        
        // 中概股
        addUSStock(stocks, "BABA", "Alibaba Group");
        addUSStock(stocks, "JD", "JD.com Inc");
        addUSStock(stocks, "PDD", "Pinduoduo Inc");
        addUSStock(stocks, "IQ", "iQIYI Inc");
        addUSStock(stocks, "BIDU", "Baidu Inc");
        addUSStock(stocks, "NIO", "NIO Inc");
        addUSStock(stocks, "XPEV", "XPeng Inc");
        addUSStock(stocks, "LI", "Li Auto Inc");
        
        System.out.println("[DEBUG] Built " + stocks.size() + " common US stocks");
        return stocks;
    }
    
    private void addUSStock(List<StockInfo> stocks, String code, String name) {
        StockInfo stock = new StockInfo();
        stock.setCode(code);
        stock.setName(name);
        stock.setJys("US");
        stocks.add(stock);
    }

    private List<StockInfo> getStockListByMarket(String marketUrl) {
        try {
            String url = marketUrl + ZTTokenHelper.getToken();
            System.out.println("[DEBUG] Fetching stock data from: " + url);
            
            String result = HttpUtil.get(url);
            System.out.println("[DEBUG] API response length: " + (result != null ? result.length() : 0));
            
            if (StringUtils.isEmpty(result)) {
                System.err.println("[ERROR] API returned empty result for: " + marketUrl);
                return Lists.newArrayList();
            }
            
            if (!JSONUtil.isTypeJSONArray(result)) {
                System.err.println("[ERROR] API response is not JSON array. First 200 chars: " + result.substring(0, Math.min(200, result.length())));
                return Lists.newArrayList();
            }
            
            List<ZTStockDTO> list = JSONUtil.toList(result, ZTStockDTO.class);
            System.out.println("[DEBUG] Parsed " + (list != null ? list.size() : 0) + " stocks from API");
            
            if (list == null || list.isEmpty()) {
                return Lists.newArrayList();
            }
            
            List<StockInfo> stockInfos = list.stream()
                .map(this::buildStockInfo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            System.out.println("[DEBUG] Built " + stockInfos.size() + " valid StockInfo objects");
            return stockInfos;
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to fetch stock data from: " + marketUrl);
            System.err.println("[ERROR] Exception: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return Lists.newArrayList();
        }
    }


    private StockInfo buildStockInfo(ZTStockDTO ztStockDTO) {
        if (Objects.isNull(ztStockDTO)) {
            return null;
        }
        String dm = ztStockDTO.getDm();
        String mc = ztStockDTO.getMc();
        String jys = ztStockDTO.getJys();
        if (StringUtils.isEmpty(dm) || StringUtils.isEmpty(mc) || StringUtils.isEmpty(jys)) {
            return null;
        }
        dm = StringUtils.substringBefore(dm, ".");
        StockInfo stockInfo = new StockInfo();
        stockInfo.setCode(dm);
        stockInfo.setName(mc);
        stockInfo.setJys(jys);
        return stockInfo;
    }
}
