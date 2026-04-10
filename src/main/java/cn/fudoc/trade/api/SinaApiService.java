package cn.fudoc.trade.api;


import cn.fudoc.trade.api.data.RealStockInfo;

import java.util.List;
import java.util.Set;

/**
 * 新浪财经API服务
 * 支持A股、港股、美股实时行情数据
 */
public interface SinaApiService {

    /**
     * 验证是否能访问新浪财经域名
     */
    void ping();

    /**
     * 股票实时数据列表（包含A股、港股、美股股票实时数据）
     *
     * @param codeSet 股票代码集合
     *                A股格式: sh600519, sz000001
     *                港股格式: hk00700, hk09988
     *                美股格式: gb_aapl, gb_tsla, gb_msft
     * @return 股票实时数据列表
     */
    List<RealStockInfo> stockList(Set<String> codeSet);
}
