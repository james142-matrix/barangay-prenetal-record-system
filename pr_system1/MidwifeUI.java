/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pr_system1;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.sql.PreparedStatement;
import java.util.Vector;
import javax.swing.*;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
/**
 *
 * @author User PC
 */
public class MidwifeUI extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MidwifeUI.class.getName());

    /**
     * Creates new form MidwifeUI
     */
    
    private int selectedMidwifeId = -1;
    
    public MidwifeUI() {
        initComponents();
        loadMidwives();
        clearFields();
        
        
        btnSave.setEnabled(true);
        btnUpdate.setEnabled(false);
        // add mouse listener to populate fields when a row is clicked
        tblMidwives.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tblMidwivesMouseClicked(e);
            }
        });
    }
    
    
private void loadMidwives() {
    DefaultTableModel model = new DefaultTableModel(
        new String[]{"ID", "Full Name", "Contact", "Address"}, 0); // removed Username column
    tblMidwives.setModel(model);

    String sql = """
        SELECT m.midwife_id, m.full_name, m.contact_no, m.address, u.user_name, u.user_pass
        FROM midwife_information m
        JOIN pr_user u ON m.user_id = u.user_id
        ORDER BY m.full_name
        """;

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement pst = conn.prepareStatement(sql);
         ResultSet rs = pst.executeQuery()) {

        while (rs.next()) {
            Vector<Object> row = new Vector<>();
            row.add(rs.getInt("midwife_id"));
            row.add(rs.getString("full_name"));
            row.add(rs.getString("contact_no"));
            row.add(rs.getString("address"));
            model.addRow(row);
        }

        // hide ID column visually
        if (tblMidwives.getColumnCount() > 0) {
            tblMidwives.getColumnModel().getColumn(0).setMinWidth(0);
            tblMidwives.getColumnModel().getColumn(0).setMaxWidth(0);
            tblMidwives.getColumnModel().getColumn(0).setWidth(0);
        }

        btnSave.setEnabled(true);
        btnUpdate.setEnabled(false);

    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Error loading midwives: " + ex.getMessage());
    }
}

    // Insert new midwife
    private void saveMidwifeWithUser() {
        String name = txtName.getText().trim();
        String contact = txtContact.getText().trim();
        String address = txtAddress.getText().trim();
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getText()).trim();

        if (name.isEmpty() || contact.isEmpty() || address.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields!");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Insert into pr_user and get generated key
            String sqlUser = "INSERT INTO pr_user (user_name, user_pass) VALUES (?, ?)";
            try (PreparedStatement psUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, username);
                psUser.setString(2, password);
                int affected = psUser.executeUpdate();
                if (affected == 0) throw new SQLException("Creating user failed, no rows affected.");

                int userId = -1;
                try (ResultSet generated = psUser.getGeneratedKeys()) {
                    if (generated.next()) {
                        userId = generated.getInt(1);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }

                // Now insert midwife with that user_id
                String sqlMidwife = "INSERT INTO midwife_information (user_id, full_name, contact_no, address) VALUES (?, ?, ?, ?)";
                try (PreparedStatement psMid = conn.prepareStatement(sqlMidwife)) {
                    psMid.setInt(1, userId);
                    psMid.setString(2, name);
                    psMid.setString(3, contact);
                    psMid.setString(4, address);
                    psMid.executeUpdate();
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Midwife and user account added successfully!");
                clearFields();
                loadMidwives();

            } catch (Exception exInner) {
                conn.rollback();
                throw exInner;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving midwife: " + e.getMessage());
            logger.log(java.util.logging.Level.SEVERE, "saveMidwifeWithUser", e);
        }
    }

    // Update selected midwife
// Update selected midwife and its linked user account
private void updateMidwife() {
    if (selectedMidwifeId <= 0) {
        JOptionPane.showMessageDialog(this, "Please select a midwife from the list to update.");
        return;
    }

    String name = txtName.getText().trim();
    String contact = txtContact.getText().trim();
    String address = txtAddress.getText().trim();
    String username = txtUsername.getText().trim();
    String password = txtPassword.getText().trim(); // if you later use JPasswordField, change accordingly

    if (name.isEmpty() || contact.isEmpty() || address.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Please fill full name, contact, and address.");
        return;
    }

    Connection conn = null;
    PreparedStatement pstGetUserId = null;
    PreparedStatement pstUpdateUser = null;
    PreparedStatement pstUpdateMidwife = null;
    ResultSet rs = null;

    try {
        conn = DBConnection.getConnection();
        conn.setAutoCommit(false); // start transaction

        // 1) get user_id from midwife_information
        String sqlGet = "SELECT user_id FROM midwife_information WHERE midwife_id = ?";
        pstGetUserId = conn.prepareStatement(sqlGet);
        pstGetUserId.setInt(1, selectedMidwifeId);
        rs = pstGetUserId.executeQuery();

        int userId = -1;
        if (rs.next()) {
            userId = rs.getInt("user_id");
        } else {
            throw new SQLException("Linked user not found for midwife_id = " + selectedMidwifeId);
        }
        rs.close();
        pstGetUserId.close();

        // 2) update pr_user if username/password provided (handle partial updates)
        if (!username.isEmpty() || !password.isEmpty()) {
            // build dynamic update depending on which fields are provided
            if (!username.isEmpty() && !password.isEmpty()) {
                String sqlUser = "UPDATE pr_user SET user_name = ?, user_pass = ? WHERE user_id = ?";
                pstUpdateUser = conn.prepareStatement(sqlUser);
                pstUpdateUser.setString(1, username);
                pstUpdateUser.setString(2, password);
                pstUpdateUser.setInt(3, userId);
                pstUpdateUser.executeUpdate();
                pstUpdateUser.close();
            } else if (!username.isEmpty()) {
                String sqlUser = "UPDATE pr_user SET user_name = ? WHERE user_id = ?";
                pstUpdateUser = conn.prepareStatement(sqlUser);
                pstUpdateUser.setString(1, username);
                pstUpdateUser.setInt(2, userId);
                pstUpdateUser.executeUpdate();
                pstUpdateUser.close();
            } else { // only password provided
                String sqlUser = "UPDATE pr_user SET user_pass = ? WHERE user_id = ?";
                pstUpdateUser = conn.prepareStatement(sqlUser);
                pstUpdateUser.setString(1, password);
                pstUpdateUser.setInt(2, userId);
                pstUpdateUser.executeUpdate();
                pstUpdateUser.close();
            }
        }

        // 3) update midwife_information
        String sqlMidwife = "UPDATE midwife_information SET full_name = ?, contact_no = ?, address = ? WHERE midwife_id = ?";
        pstUpdateMidwife = conn.prepareStatement(sqlMidwife);
        pstUpdateMidwife.setString(1, name);
        pstUpdateMidwife.setString(2, contact);
        pstUpdateMidwife.setString(3, address);
        pstUpdateMidwife.setInt(4, selectedMidwifeId);
        pstUpdateMidwife.executeUpdate();
        pstUpdateMidwife.close();

        conn.commit();

        JOptionPane.showMessageDialog(this, "Midwife and account updated successfully!");
        clearFields();
        loadMidwives();

    } catch (Exception ex) {
        try {
            if (conn != null) conn.rollback();
        } catch (SQLException se) {
            // ignore rollback error
        }
        JOptionPane.showMessageDialog(this, "Error updating midwife: " + ex.getMessage());
        logger.log(java.util.logging.Level.SEVERE, "updateMidwife", ex);
    } finally {
        try { if (rs != null) rs.close(); } catch (Exception ignore) {}
        try { if (pstGetUserId != null) pstGetUserId.close(); } catch (Exception ignore) {}
        try { if (pstUpdateUser != null) pstUpdateUser.close(); } catch (Exception ignore) {}
        try { if (pstUpdateMidwife != null) pstUpdateMidwife.close(); } catch (Exception ignore) {}
        try { if (conn != null) conn.setAutoCommit(true); conn.close(); } catch (Exception ignore) {}
    }
}

    // Delete selected midwife

    // clear input fields
    private void clearFields() {
        txtName.setText("");
        txtContact.setText("");
        txtAddress.setText("");
        txtUsername.setText("");
        txtPassword.setText("");
        selectedMidwifeId = -1;
        txtName.requestFocus();
        btnSave.setEnabled(true);
        btnUpdate.setEnabled(false);
    }

    // populate fields when user clicks a table row
    private void tableRowSelected() {
        int row = tblMidwives.getSelectedRow();
        if (row == -1) return;
        DefaultTableModel model = (DefaultTableModel) tblMidwives.getModel();

        // id is in column 0 (hidden)
        selectedMidwifeId = (int) model.getValueAt(row, 0);
        jtext.setText((String) model.getValueAt(row, 1));
        txtContact.setText((String) model.getValueAt(row, 3));
        txtAddress.setText((String) model.getValueAt(row, 4));

        btnSave.setEnabled(false);
        btnUpdate.setEnabled(true);
    }
    
private void tblMidwivesMouseClicked(java.awt.event.MouseEvent evt) {                                         
    int row = tblMidwives.getSelectedRow();
    if (row != -1) {
        DefaultTableModel model = (DefaultTableModel) tblMidwives.getModel();
        selectedMidwifeId = Integer.parseInt(model.getValueAt(row, 0).toString());
        txtName.setText(model.getValueAt(row, 1).toString());
        txtContact.setText(model.getValueAt(row, 2).toString());
        txtAddress.setText(model.getValueAt(row, 3).toString());

        // Now fetch username and password from database
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                 "SELECT u.user_name, u.user_pass " +
                 "FROM midwife_information m " +
                 "JOIN pr_user u ON m.user_id = u.user_id " +
                 "WHERE m.midwife_id = ?")) {

            pst.setInt(1, selectedMidwifeId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                txtUsername.setText(rs.getString("user_name"));
                txtPassword.setText(rs.getString("user_pass"));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading user details: " + e.getMessage());
        }

        btnSave.setEnabled(false);
        btnUpdate.setEnabled(true);
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

        lblTitle = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        lblTitle1 = new javax.swing.JLabel();
        jtext = new javax.swing.JLabel();
        lblTitle3 = new javax.swing.JLabel();
        lblTitle4 = new javax.swing.JLabel();
        lblTitle5 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblMidwives = new javax.swing.JTable();
        txtName = new javax.swing.JTextField();
        txtContact = new javax.swing.JTextField();
        txtAddress = new javax.swing.JTextField();
        txtName1 = new javax.swing.JLabel();
        btnUpdate = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        btnClear = new javax.swing.JButton();
        lblTitle6 = new javax.swing.JLabel();
        lblTitle7 = new javax.swing.JLabel();
        txtUsername = new javax.swing.JTextField();
        txtPassword = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();

        lblTitle.setFont(new java.awt.Font("Arial", 1, 48)); // NOI18N
        lblTitle.setText("Medical History");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lblTitle1.setFont(new java.awt.Font("Arial", 1, 48)); // NOI18N
        lblTitle1.setForeground(new java.awt.Color(255, 255, 255));
        lblTitle1.setText("Midwives Information");
        jPanel1.add(lblTitle1, new org.netbeans.lib.awtextra.AbsoluteConstraints(255, 61, -1, -1));

        jtext.setFont(new java.awt.Font("Arial", 1, 20)); // NOI18N
        jtext.setForeground(new java.awt.Color(255, 255, 255));
        jtext.setText("Midwives Information");
        jPanel1.add(jtext, new org.netbeans.lib.awtextra.AbsoluteConstraints(87, 169, -1, -1));

        lblTitle3.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lblTitle3.setForeground(new java.awt.Color(255, 255, 255));
        lblTitle3.setText("Full Name:");
        jPanel1.add(lblTitle3, new org.netbeans.lib.awtextra.AbsoluteConstraints(87, 209, -1, -1));

        lblTitle4.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lblTitle4.setForeground(new java.awt.Color(255, 255, 255));
        lblTitle4.setText("Contact No. :");
        jPanel1.add(lblTitle4, new org.netbeans.lib.awtextra.AbsoluteConstraints(87, 250, -1, -1));

        lblTitle5.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lblTitle5.setForeground(new java.awt.Color(255, 255, 255));
        lblTitle5.setText("Address:");
        jPanel1.add(lblTitle5, new org.netbeans.lib.awtextra.AbsoluteConstraints(87, 291, -1, -1));

        tblMidwives.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(tblMidwives);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(87, 455, 839, 205));

        txtName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNameActionPerformed(evt);
            }
        });
        jPanel1.add(txtName, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 210, 305, -1));

        txtContact.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtContactActionPerformed(evt);
            }
        });
        jPanel1.add(txtContact, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 251, 305, -1));

        txtAddress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtAddressActionPerformed(evt);
            }
        });
        jPanel1.add(txtAddress, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 292, 305, -1));

        txtName1.setFont(new java.awt.Font("Arial", 1, 20)); // NOI18N
        txtName1.setForeground(new java.awt.Color(255, 255, 255));
        txtName1.setText("List of Midwives");
        jPanel1.add(txtName1, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 420, -1, -1));

        btnUpdate.setText("Update");
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });
        jPanel1.add(btnUpdate, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 678, 90, 46));

        btnBack.setText("Back");
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });
        jPanel1.add(btnBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(836, 678, 90, 46));

        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        jPanel1.add(btnSave, new org.netbeans.lib.awtextra.AbsoluteConstraints(512, 678, 90, 46));

        btnClear.setText("Clear");
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });
        jPanel1.add(btnClear, new org.netbeans.lib.awtextra.AbsoluteConstraints(728, 678, 90, 46));

        lblTitle6.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lblTitle6.setForeground(new java.awt.Color(255, 255, 255));
        lblTitle6.setText("Username:");
        jPanel1.add(lblTitle6, new org.netbeans.lib.awtextra.AbsoluteConstraints(87, 332, -1, -1));

        lblTitle7.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lblTitle7.setForeground(new java.awt.Color(255, 255, 255));
        lblTitle7.setText("Password:");
        jPanel1.add(lblTitle7, new org.netbeans.lib.awtextra.AbsoluteConstraints(87, 373, -1, -1));

        txtUsername.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtUsernameActionPerformed(evt);
            }
        });
        jPanel1.add(txtUsername, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 333, 305, -1));

        txtPassword.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPasswordActionPerformed(evt);
            }
        });
        jPanel1.add(txtPassword, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 374, 305, -1));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pr_system1/27.jpg"))); // NOI18N
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(-490, 0, 1500, 760));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        // TODO add your handling code here:
        updateMidwife();    
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        // TODO add your handling code here:
        new MainMenu().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        // TODO add your handling code here:
        saveMidwifeWithUser();
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        // TODO add your handling code here:
        clearFields();
    }//GEN-LAST:event_btnClearActionPerformed

    private void txtNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNameActionPerformed

    private void txtContactActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtContactActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtContactActionPerformed

    private void txtAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtAddressActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtAddressActionPerformed

    private void txtUsernameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtUsernameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtUsernameActionPerformed

    private void txtPasswordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPasswordActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtPasswordActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new MidwifeUI().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel jtext;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblTitle1;
    private javax.swing.JLabel lblTitle3;
    private javax.swing.JLabel lblTitle4;
    private javax.swing.JLabel lblTitle5;
    private javax.swing.JLabel lblTitle6;
    private javax.swing.JLabel lblTitle7;
    private javax.swing.JTable tblMidwives;
    private javax.swing.JTextField txtAddress;
    private javax.swing.JTextField txtContact;
    private javax.swing.JTextField txtName;
    private javax.swing.JLabel txtName1;
    private javax.swing.JTextField txtPassword;
    private javax.swing.JTextField txtUsername;
    // End of variables declaration//GEN-END:variables
}
