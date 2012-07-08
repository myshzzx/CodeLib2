/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mysh.codelib2.ui;

import javax.swing.DefaultListModel;

/**
 *
 * @author Allen
 */
public final class CodeLib2Main extends javax.swing.JPanel {

    public static interface AppTitltSetter {

        void setTitle(String title);
    }

    /**
     * Creates new form CodeLib2Main
     */
    public CodeLib2Main() {
        initComponents();

        this.controllor = new UIControllor(this);
    }

    public CodeLib2Main setAppTitleSetter(AppTitltSetter appTitltSetter) {
        this.appTitltSetter = appTitltSetter;
        return this;
    }

    public void setAppTitle(String title) {
        if (this.appTitltSetter != null) {
            this.appTitltSetter.setTitle(title);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToolBar1 = new javax.swing.JToolBar();
        newInst = new javax.swing.JButton();
        open = new javax.swing.JButton();
        save = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        add = new javax.swing.JButton();
        remove = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        export = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        filterText = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        resultList = new javax.swing.JList();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel2 = new javax.swing.JPanel();
        keyWordText = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        codeText = new org.fife.ui.rsyntaxtextarea.RSyntaxTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        attachmentList = new javax.swing.JTable();
        stateBar = new javax.swing.JLabel();

        setFont(getFont());

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        newInst.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mysh/codelib2/ui/icons/new.png"))); // NOI18N
        newInst.setToolTipText("新建");
        newInst.setFocusable(false);
        newInst.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        newInst.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        newInst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newInstActionPerformed(evt);
            }
        });
        jToolBar1.add(newInst);

        open.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mysh/codelib2/ui/icons/open.png"))); // NOI18N
        open.setToolTipText("打开");
        open.setFocusable(false);
        open.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        open.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        open.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openActionPerformed(evt);
            }
        });
        jToolBar1.add(open);

        save.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mysh/codelib2/ui/icons/save.png"))); // NOI18N
        save.setToolTipText("保存");
        save.setFocusable(false);
        save.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        save.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionPerformed(evt);
            }
        });
        jToolBar1.add(save);
        jToolBar1.add(jSeparator1);

        add.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mysh/codelib2/ui/icons/add.png"))); // NOI18N
        add.setToolTipText("新增");
        add.setFocusable(false);
        add.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        add.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        add.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addActionPerformed(evt);
            }
        });
        jToolBar1.add(add);

        remove.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mysh/codelib2/ui/icons/remove.png"))); // NOI18N
        remove.setToolTipText("移除");
        remove.setFocusable(false);
        remove.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        remove.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        remove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeActionPerformed(evt);
            }
        });
        jToolBar1.add(remove);
        jToolBar1.add(jSeparator2);

        export.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mysh/codelib2/ui/icons/export.png"))); // NOI18N
        export.setToolTipText("导出");
        export.setFocusable(false);
        export.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        export.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        export.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportActionPerformed(evt);
            }
        });
        jToolBar1.add(export);

        jSplitPane1.setDividerLocation(220);
        jSplitPane1.setDividerSize(7);

        jPanel1.setFont(new java.awt.Font("Microsoft YaHei", 0, 12)); // NOI18N

        filterText.setFont(new java.awt.Font("Microsoft YaHei", 0, 12)); // NOI18N
        filterText.setToolTipText("空格或逗号分隔搜索关键字, ESC 复位");
        filterText.setNextFocusableComponent(resultList);

        resultList.setModel(new DefaultListModel<>());
        resultList.setFont(new java.awt.Font("Microsoft YaHei", 0, 12)); // NOI18N
        resultList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        resultList.setNextFocusableComponent(keyWordText);
        resultList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                resultListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(resultList);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(filterText)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 219, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(filterText, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE))
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jSplitPane2.setDividerLocation(300);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane2.setResizeWeight(0.7);

        keyWordText.setFont(new java.awt.Font("Microsoft YaHei", 0, 12)); // NOI18N
        keyWordText.setToolTipText("逗号分隔关键字, 如 java, GUI, tree");
        keyWordText.setNextFocusableComponent(codeText);

        codeText.setColumns(20);
        codeText.setRows(5);
        codeText.setCodeFoldingEnabled(true);
        codeText.setFont(new java.awt.Font("Microsoft YaHei", 0, 14)); // NOI18N
        codeText.setFractionalFontMetricsEnabled(true);
        codeText.setMargin(new java.awt.Insets(4, 4, 4, 4));
        codeText.setNextFocusableComponent(attachmentList);
        jScrollPane3.setViewportView(codeText);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(keyWordText)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 423, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(keyWordText, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE))
        );

        jSplitPane2.setTopComponent(jPanel2);

        attachmentList.setFont(new java.awt.Font("Microsoft YaHei", 0, 12)); // NOI18N
        attachmentList.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Size"
            }
        ));
        jScrollPane4.setViewportView(attachmentList);

        jSplitPane2.setRightComponent(jScrollPane4);

        jSplitPane1.setRightComponent(jSplitPane2);

        stateBar.setFont(new java.awt.Font("Microsoft YaHei", 0, 14)); // NOI18N
        stateBar.setText("就绪.");
        stateBar.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        stateBar.setVerticalTextPosition(javax.swing.SwingConstants.TOP);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 653, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(stateBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stateBar, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void newInstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newInstActionPerformed
        this.controllor.newInst();
    }//GEN-LAST:event_newInstActionPerformed

    private void openActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openActionPerformed
        this.controllor.open();
    }//GEN-LAST:event_openActionPerformed

    private void saveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveActionPerformed
        this.controllor.save();
    }//GEN-LAST:event_saveActionPerformed

    private void addActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addActionPerformed
        this.controllor.addItem();
    }//GEN-LAST:event_addActionPerformed

    private void removeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeActionPerformed
        this.controllor.removeItem();
    }//GEN-LAST:event_removeActionPerformed

    private void exportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportActionPerformed
        this.controllor.export();
    }//GEN-LAST:event_exportActionPerformed

    private void resultListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_resultListValueChanged
        this.controllor.selectItem(this.resultList.getSelectedValue());
    }//GEN-LAST:event_resultListValueChanged
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton add;
    javax.swing.JTable attachmentList;
    org.fife.ui.rsyntaxtextarea.RSyntaxTextArea codeText;
    private javax.swing.JButton export;
    javax.swing.JTextField filterText;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JToolBar jToolBar1;
    javax.swing.JTextField keyWordText;
    private javax.swing.JButton newInst;
    private javax.swing.JButton open;
    private javax.swing.JButton remove;
    javax.swing.JList resultList;
    private javax.swing.JButton save;
    javax.swing.JLabel stateBar;
    // End of variables declaration//GEN-END:variables
    private UIControllor controllor;
    private AppTitltSetter appTitltSetter;
}
