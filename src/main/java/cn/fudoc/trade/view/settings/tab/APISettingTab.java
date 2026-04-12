package cn.fudoc.trade.view.settings.tab;

import cn.fudoc.trade.core.state.FuStockSettingState;
import cn.fudoc.trade.util.FormPanelUtil;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

/**
 * API 配置设置页面
 */
public class APISettingTab implements SettingTab {
    
    private final JBTextField iTickTokenField = new JBTextField();
    
    public APISettingTab() {
        // 加载当前配置
        FuStockSettingState instance = FuStockSettingState.getInstance();
        iTickTokenField.setText(instance.getITickApiToken());
        
        // 设置占位符提示
        iTickTokenField.setToolTipText("iTick API Token，用于获取美股实时行情数据");
    }
    
    @Override
    public String getTabName() {
        return "API配置";
    }
    
    @Override
    public JPanel createPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(JBUI.Borders.empty(20, 30));
        
        // 添加 iTick API Token 配置项
        FormPanelUtil.addRow(mainPanel, "iTick API Token", iTickTokenField);
        
        // 添加说明信息
        JLabel infoLabel = new JLabel("<html><body style='width: 400px;'>" +
                "<p><b>iTick API</b> 提供更准确的美股实时行情数据</p>" +
                "<p>注册地址: <a href='https://itick.io'>https://itick.io</a></p>" +
                "<p>免费套餐: 无限调用基本行情</p>" +
                "<p><font color='gray'>如果不配置，将自动使用新浪 API 作为备用数据源</font></p>" +
                "</body></html>");
        infoLabel.setBorder(JBUI.Borders.emptyTop(10));
        mainPanel.add(infoLabel);
        
        return mainPanel;
    }
    
    @Override
    public ValidationInfo doValidate() {
        // Token 可以为空，不做强制校验
        return null;
    }
    
    @Override
    public void submit() {
        FuStockSettingState instance = FuStockSettingState.getInstance();
        String token = iTickTokenField.getText();
        if (token != null) {
            instance.setITickApiToken(token.trim());
        } else {
            instance.setITickApiToken("");
        }
    }
}
