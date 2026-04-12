package cn.fudoc.trade.test;

import cn.fudoc.trade.api.ZTApiService;
import cn.fudoc.trade.api.data.StockInfo;
import cn.fudoc.trade.core.state.index.StockIndex;
import com.intellij.openapi.application.ApplicationManager;

import java.util.List;

/**
 * 美股搜索功能测试
 */
public class USStockSearchTest {
    
    public static void main(String[] args) {
        System.out.println("=== 开始测试美股搜索功能 ===");
        
        // 1. 获取美股数据
        ZTApiService ztApiService = ApplicationManager.getApplication().getService(ZTApiService.class);
        List<StockInfo> usStocks = ztApiService.marketUS();
        
        System.out.println("获取到美股数量: " + (usStocks != null ? usStocks.size() : 0));
        
        if (usStocks != null && !usStocks.isEmpty()) {
            // 打印前5只股票信息
            System.out.println("\n前5只美股信息:");
            for (int i = 0; i < Math.min(5, usStocks.size()); i++) {
                StockInfo stock = usStocks.get(i);
                System.out.println(String.format(
                    "  [%d] code=%s, name=%s, jys=%s, stockCode=%s",
                    i + 1,
                    stock.getCode(),
                    stock.getName(),
                    stock.getJys(),
                    stock.getStockCode()
                ));
            }
            
            // 2. 构建索引
            System.out.println("\n构建美股索引...");
            StockIndex usIndex = new StockIndex(usStocks, false, true);
            
            // 3. 测试搜索 NVDA
            System.out.println("\n测试搜索 'NVDA':");
            var results = usIndex.match("NVDA");
            System.out.println("搜索结果数量: " + results.size());
            for (var result : results) {
                System.out.println(String.format(
                    "  - %s (%s) similarity=%.2f",
                    result.getStockInfo().getName(),
                    result.getStockInfo().getCode(),
                    result.getSimilarity()
                ));
            }
            
            // 4. 测试搜索 nvda（小写）
            System.out.println("\n测试搜索 'nvda' (小写):");
            results = usIndex.match("nvda");
            System.out.println("搜索结果数量: " + results.size());
            for (var result : results) {
                System.out.println(String.format(
                    "  - %s (%s) similarity=%.2f",
                    result.getStockInfo().getName(),
                    result.getStockInfo().getCode(),
                    result.getSimilarity()
                ));
            }
            
            // 5. 检查索引映射
            System.out.println("\n索引统计:");
            System.out.println("  codeMap size: " + (usIndex.getCodeMap() != null ? usIndex.getCodeMap().size() : 0));
            System.out.println("  firstMap size: " + (usIndex.getFirstMap() != null ? usIndex.getFirstMap().size() : 0));
            System.out.println("  nameMap size: " + (usIndex.getNameMap() != null ? usIndex.getNameMap().size() : 0));
            
            // 检查 firstMap 中是否有 nvda
            if (usIndex.getFirstMap() != null) {
                System.out.println("\nfirstMap 中的键示例 (前10个):");
                usIndex.getFirstMap().keySet().stream()
                    .limit(10)
                    .forEach(key -> System.out.println("  - " + key));
                
                // 特别检查 nvda
                if (usIndex.getFirstMap().containsKey("nvda")) {
                    System.out.println("\n✓ firstMap 包含 'nvda' 键");
                    System.out.println("  对应的代码: " + usIndex.getFirstMap().get("nvda"));
                } else {
                    System.out.println("\n✗ firstMap 不包含 'nvda' 键");
                }
            }
        } else {
            System.out.println("错误: 未能获取美股数据！");
        }
        
        System.out.println("\n=== 测试完成 ===");
    }
}
