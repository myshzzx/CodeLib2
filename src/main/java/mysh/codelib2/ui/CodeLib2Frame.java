/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mysh.codelib2.ui;

import mysh.codelib2.model.CodeLib2Element;
import mysh.util.UIUtil;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 *
 * @author Allen
 */
public class CodeLib2Frame extends javax.swing.JFrame {

    private final CodeLib2Main codeLib2Main;

    /**
     * Creates new form CodeLib2Frame
     */
    public CodeLib2Frame() {
        initComponents();
	    this.setTitle(UIControllor.AppTitle);
	    this.setIconImage(Toolkit.getDefaultToolkit().getImage(
					    this.getClass().getClassLoader().getResource("icons/CodeLib2.png")));
	    System.setProperty("file.encoding", CodeLib2Element.DefaultCharsetEncode);

      this.getContentPane().setLayout(new BorderLayout());
      this.codeLib2Main = new CodeLib2Main().setAppTitleSetter(new CodeLib2Main.AppTitltSetter() {

          @Override
          public void setTitle(String title) {
              CodeLib2Frame.this.setTitle(title);
          }
      });
      this.getContentPane().add(this.codeLib2Main, BorderLayout.CENTER);

      this.setLocationRelativeTo(null);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("CodeLib2");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1046, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 627, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (this.codeLib2Main.doClose()) {
            this.dispose();
        }
    }//GEN-LAST:event_formWindowClosing

    /**
     * @param args the command line arguments
     */
    public static void main(final String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CodeLib2Frame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

	    UIUtil.resetFont(null);

        /*
         * Create and display the form
         */
        frame = new CodeLib2Frame();
        frame.setVisible(true);

        if (args.length > 0) {
            frame.codeLib2Main.openFile(new File(args[0]));
        }

    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    private static CodeLib2Frame frame;
    public static void shutdown(){
        WindowEvent we = new WindowEvent(frame, WindowEvent.WINDOW_CLOSING);
        frame.dispatchEvent(we);
    }
}
