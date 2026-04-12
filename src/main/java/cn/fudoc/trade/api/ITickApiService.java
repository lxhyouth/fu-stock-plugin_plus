package cn.fudoc.trade.api;

import cn.fudoc.trade.api.data.RealStockInfo;

import java.util.List;
import java.util.Set;

/**
 * iTick API 服务接口
 * 提供美股实时行情数据
 */
public interface ITickApiService {
    
    /**
     * 获取股票实时行情
     * @param codeSet 股票代码集合（如：AAPL, NVDA, IQ）
     * @return 实时股票信息列表
     */
    List<RealStockInfo> stockList(Set<String> codeSet);
}
