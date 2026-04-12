
# fu-stock-plugin

[![JetBrains Plugins](https://plugins.jetbrains.com/files/29372/913770/icon/default.svg)](https://plugins.jetbrains.com/plugin/29372-fustock)
[![Version](http://phpstorm.espend.de/badge/19269/version)](https://plugins.jetbrains.com/plugin/29372-fuStock/versions)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/29372-fustock.svg)](https://plugins.jetbrains.com/plugin/29372-fustock)
[![License](https://img.shields.io/badge/license-MIT-red.svg)]()


> FuStock是一个可以实时查看A股、港股和美股股票行情，维护自己股票持仓的炒股插件。支持多分组管理，实时刷新股票行情，股票每日收益信息等

> FuStock默认3s刷新一次，只有展开侧边栏并且开启自动刷新时才会实时刷新，当隐藏侧边栏时，会默认停止自动刷新，避免大量无效请求

> 目前仅支持IDEA 2022及以后版本

## 🎉 v2.1 更新亮点

### ✨ 新增功能
- **股票列表增强**：自选股列表新增“最高价”和“最低价”两列，提供更完整的行情信息
- **多数据源支持**：集成 iTick API、新浪API、腾讯API，智能路由确保美股数据准确性
- **配置管理界面**：新增“API配置”设置页面，可灵活配置第三方数据源 Token

### 🐛 Bug 修复
- 修复美股数据解析错误（iTick API 字段映射问题）
- 修复股票搜索重复结果问题
- 修复隐藏模式与正常模式显示不一致问题
- 修复美股成交额显示错误（统一使用 values[30] 字段）
- 优化成交额格式化，所有单位统一保留两位小数

### 🔧 技术优化
- 完善多数据源降级策略：iTick → 新浪 → 腾讯
- 增强调试日志，便于问题排查
- 优化股票代码索引，支持多种格式搜索

特性
---
- 支持自定义维护自选分组，持仓分组（高级展示，自动实时计算收益）
- 支持今日收益展示，今日收益排行榜展示
- 支持维护持仓成本，今日买入，卖出等交易，实时计算持仓成本和今日收益
- 支持隐蔽模式：不展示红绿色字体，隐藏中文，和你的代码融为一体，外人即使盯着看也无法分辨
- 支持正常模式：高亮红绿色字体，高级展示持仓信息
- 支持交易费率设置，根据交易费用自动分摊到成本，让你的数据和实际交易保持一致
- 支持自定义维护中文映射，在隐蔽模式时，页面所有中文内容都可以自定义展示
- **新增**：股票列表显示最高价和最低价，提供更完整的行情信息
- **新增**：支持多数据源智能路由（iTick API、新浪API、腾讯API），美股数据更准确
- **新增**：API配置管理界面，可灵活配置第三方数据源 Token

快速开始
----
![img.png](img/holdings.png)
![img.png](img/watch-list.png)
#### 安装插件
![img.png](/img/install.png)

#### 添加股票分组
![img.png](img/add-group.png)
![img.png](img/add-hold.png)
#### 添加股票
![img.png](img/add-stock.png)

#### 今日交易
![img.png](img/sell.png)

![img.png](img/logList.png)

未来计划
---
- 新增隐藏模式，去掉颜色以及中文拼音化，只保留关键信息展示，让FuStock与你的代码融为一体
- 新增大盘指数实时展示，每日全市涨跌数量，成交额展示
- 支持ETF，基金，黄金等实时数据展示
- 新增板块数据实时展示
- 支持买入，卖出记录实时交易信息
- 支持IDEA底部栏实时展示当天利润

