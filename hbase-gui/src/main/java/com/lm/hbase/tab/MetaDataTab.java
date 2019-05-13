package com.lm.hbase.tab;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.SystemColor;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneLayout;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import com.alibaba.fastjson.JSON;
import com.lm.hbase.common.Env;
import com.lm.hbase.conf.HbaseClientConf;
import com.lm.hbase.swing.HandleCore;
import com.lm.hbase.swing.HbaseGui;
import com.lm.hbase.util.StringUtil;

public class MetaDataTab extends TabAbstract {

    private JList<String> list;
    private JTable        contentTable;
    private JScrollPane   tableScroll;
    private JButton       saveButton;

    public MetaDataTab(HbaseGui window){
        super(window);
    }

    @Override
    public String getTitle() {
        return "元数据维护";
    }

    @Override
    public JPanel initializePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 0));

        // 展示数据库列表的panel
        JPanel tableListPanel = new JPanel();
        tableListPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        tableListPanel.setLayout(new BorderLayout(1, 1));

        panel.add(tableListPanel, BorderLayout.WEST);

        list = new JList<>();
        list.setFixedCellHeight(20);
        list.setBackground(SystemColor.window);
        // 设置为单选模式
        list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane jlistScroll = new JScrollPane(list);
        jlistScroll.setLayout(new ScrollPaneLayout());
        tableListPanel.add(jlistScroll);

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setToolTipText("");
        popupMenu.setAlignmentX(Component.CENTER_ALIGNMENT);
        addPopup(list, popupMenu);

        JButton refreshTableButton = new JButton("刷新", new ImageIcon(Env.IMG_DIR + "Search.png"));
        tableListPanel.add(refreshTableButton, BorderLayout.NORTH);

        JPanel southPanel = new JPanel();
        southPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        southPanel.setLayout(new BorderLayout(0, 0));
        panel.add(southPanel, BorderLayout.CENTER);

        contentTable = new JTable();
        tableScroll = new JScrollPane(contentTable);
        southPanel.add(tableScroll, BorderLayout.CENTER);

        JPanel searchSouthPanel = new JPanel();
        searchSouthPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        southPanel.add(searchSouthPanel, BorderLayout.SOUTH);
        searchSouthPanel.setLayout(new BorderLayout(0, 0));

        saveButton = new JButton("保存元数据", new ImageIcon(Env.IMG_DIR + "save.png"));
        searchSouthPanel.add(saveButton, BorderLayout.EAST);

        /*
         * 添加各种监听
         */

        // 给刷新按钮添加监听
        refreshTableButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                refreshTableButton.setEnabled(false);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                initTableList(list);
                refreshTableButton.setEnabled(true);
            }
        });

        // 表格自适应监听
        contentTable.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                resizeTable(true, contentTable, tableScroll);
            }
        });

        // 选中hbase表时的监听
        list.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    loadMataData(list.getSelectedValue());
                }

            }
        });

        saveButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                Map<String, String> map = new HashMap<>();
                for (int i = 0; i < contentTable.getRowCount(); i++) {
                    map.put(contentTable.getValueAt(i, 0).toString(), contentTable.getValueAt(i, 1).toString());
                }

                String propertiesKey = list.getSelectedValue() + PROPERTIES_SUFFIX;
                HbaseClientConf.setValue(propertiesKey, JSON.toJSONString(map));
                JOptionPane.showMessageDialog(getFrame(), "保存成功", "提示", JOptionPane.INFORMATION_MESSAGE);

            }
        });

        // 初始化表
        initTableList(list);

        return panel;
    }

    @SuppressWarnings("unchecked")
    private void loadMataData(String tableName) {
        startTask();
        getSingleThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                // 清空table
                DefaultTableModel tableModel = (DefaultTableModel) contentTable.getModel();
                tableModel.setRowCount(0);
                // 1、 优先从映射文件中查询元数据
                // 2、查询不到则去Hbase查询表结构，得到所有的字段。所有字段默认是string类型
                String propertiesKey = list.getSelectedValue() + PROPERTIES_SUFFIX;
                String cacheMetaData = HbaseClientConf.getStringValue(propertiesKey);
                if (!StringUtil.isEmpty(cacheMetaData)) {
                    HandleCore.reloadMetaTableFormat(contentTable, JSON.parseObject(cacheMetaData, Map.class));
                } else {
                    HandleCore.reloadMetaTableFormat(tableName, contentTable);
                }
                stopTask();

            }
        });

    }

    @Override
    public void enableAll() {
        list.setEnabled(true);
    }

    @Override
    public void disableAll() {
        list.setEnabled(false);
    }

}