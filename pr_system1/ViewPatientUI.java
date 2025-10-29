/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pr_system1;

import java.sql.PreparedStatement;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
/**
 *
 * @author User PC
 */
public class ViewPatientUI extends javax.swing.JFrame {
    private int patientId; // passed from previous form

    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ViewPatientUI.class.getName());

    public ViewPatientUI(int patientId) {
        initComponents();
        txtPersonalInfo.setEditable(false);
        txtPregnancyInfo.setEditable(false);
        txtCheckupDetails.setEditable(false);
        txtMedicalHistory.setEditable(false);
        this.patientId = patientId;
        loadPatientDetails();
        loadMedicalHistory(patientId);
    }
    
private void loadPatientDetails() {
    try (Connection conn = DBConnection.getConnection()) {

        // 🧍 PERSONAL INFORMATION + MIDWIFE JOIN
        PreparedStatement pst1 = conn.prepareStatement(
            "SELECT p.*, m.full_name AS midwife_name " +
            "FROM patient_information p " +
            "LEFT JOIN midwife_information m ON p.midwife_id = m.midwife_id " +
            "WHERE p.patient_id = ?");
        pst1.setInt(1, patientId);
        ResultSet rs1 = pst1.executeQuery();

        if (rs1.next()) {
            String fullName = rs1.getString("first_name") + " " + rs1.getString("last_name");
            lblName.setText("Patient Record: " + fullName);

            String midwifeName = rs1.getString("midwife_name");
            if (midwifeName == null || midwifeName.isEmpty()) {
                midwifeName = "Not Assigned";
            }

            txtPersonalInfo.setText(
                "Age: " + rs1.getInt("age") + "\n" +
                "Birth Date: " + rs1.getString("birth_date") + "\n" +
                "Address: " + rs1.getString("address") + "\n" +
                "Contact: " + rs1.getString("contact_no") + "\n" +
                "Blood Type: " + rs1.getString("blood_type") + "\n" +
                "Assigned Midwife: " + midwifeName
            );
        }

        // 🤰 PREGNANCY INFORMATION
        PreparedStatement pst2 = conn.prepareStatement(
            "SELECT * FROM pregnancy_information WHERE patient_id = ?");
        pst2.setInt(1, patientId);
        ResultSet rs2 = pst2.executeQuery();

        if (rs2.next()) {
            txtPregnancyInfo.setText(
                "Gravida: " + rs2.getInt("gravida") + " | Para: " + rs2.getInt("para") + "\n" +
                "LMP: " + rs2.getString("lmp") + "\n" +
                "EED: " + rs2.getString("eed")
            );
        }

        // 🩺 MEDICAL HISTORY TABLE
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Hypertension", "Diabetes", "Asthma", "Heart Disease", "C-Section", "Allergies", "Other Conditions"}, 0);
        tblVisitHistory.setModel(model);
        tblVisitHistory.getTableHeader().setReorderingAllowed(false);
        tblVisitHistory.setDefaultEditor(Object.class, null);

        PreparedStatement pst3 = conn.prepareStatement(
            "SELECT * FROM medical_history WHERE patient_id = ?");
        pst3.setInt(1, patientId);
        ResultSet rs3 = pst3.executeQuery();

while (rs3.next()) {
    String hypertension = rs3.getInt("hypertension") == 1 ? "Yes" : "No";
    String diabetes = rs3.getInt("diabetes") == 1 ? "Yes" : "No";
    String asthma = rs3.getInt("asthma") == 1 ? "Yes" : "No";
    String heart = rs3.getInt("heart_disease") == 1 ? "Yes" : "No";
    String cs = rs3.getInt("previous_cs") == 1 ? "Yes" : "No";
    String allergies = rs3.getString("allergies");
    String other = rs3.getString("other_conditions");

    model.addRow(new Object[]{hypertension, diabetes, asthma, heart, cs, allergies, other});
}

        // 🩻 LATEST CHECKUP DETAILS
        PreparedStatement pst4 = conn.prepareStatement(
            "SELECT * FROM checkup_details WHERE patient_id = ? ORDER BY date_of_visit DESC LIMIT 1");
        pst4.setInt(1, patientId);
        ResultSet rs4 = pst4.executeQuery();

        if (rs4.next()) {
            txtCheckupDetails.setText(
                "Date of Visit: " + rs4.getString("date_of_visit") + "\n" +
                "Blood Pressure: " + rs4.getString("blood_pressure") + "\n" +
                "Weight: " + rs4.getDouble("weight") + "\n" +
                "Fetal Heart Rate: " + rs4.getInt("fetal_hr") + "\n" +
                "Notes: " + rs4.getString("notes")
            );
        } else {
            txtCheckupDetails.setText("No checkup record available.");
        }

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error loading patient data: " + e.getMessage());
    }
}

    
    private void loadMedicalHistory(int patientId) {
        try (Connection conn = DBConnection.getConnection()) {
        String sql = "SELECT * FROM medical_history WHERE patient_id = ?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, patientId);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            // Convert 0/1 to Yes/No
            String hypertension = rs.getInt("hypertension") == 1 ? "Yes" : "No";
            String diabetes = rs.getInt("diabetes") == 1 ? "Yes" : "No";
            String asthma = rs.getInt("asthma") == 1 ? "Yes" : "No";
            String heart = rs.getInt("heart_disease") == 1 ? "Yes" : "No";
            String cs = rs.getInt("previous_cs") == 1 ? "Yes" : "No";
            String allergies = rs.getString("allergies");
            String other = rs.getString("other_conditions");

            // Display in JTextArea
            txtMedicalHistory.setText(
                "Hypertension: " + hypertension + "\n" +
                "Diabetes: " + diabetes + "\n" +
                "Asthma: " + asthma + "\n" +
                "Heart Disease: " + heart + "\n" +
                "Previous C-Section: " + cs + "\n" +
                "Allergies: " + allergies + "\n" +
                "Other Conditions: " + other
            );
        } else {
            txtMedicalHistory.setText("No medical history found for this patient.");
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error loading medical history: " + e.getMessage());
    }
}

    /**
     * Creates new form ViewPatientUI
     */
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        lblName = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtPersonalInfo = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtPregnancyInfo = new javax.swing.JTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblVisitHistory = new javax.swing.JTable();
        btnBack = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        txtCheckupDetails = new javax.swing.JTextArea();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        txtMedicalHistory = new javax.swing.JTextArea();
        jLabel6 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lblTitle.setFont(new java.awt.Font("Arial", 1, 30)); // NOI18N
        lblTitle.setForeground(new java.awt.Color(255, 255, 255));
        lblTitle.setText("Patient Record");
        jPanel1.add(lblTitle, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 30, 255, -1));

        lblName.setFont(new java.awt.Font("Arial", 1, 24)); // NOI18N
        lblName.setForeground(new java.awt.Color(255, 255, 255));
        lblName.setText("jLabel1");
        jPanel1.add(lblName, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 90, 366, 31));

        txtPersonalInfo.setColumns(20);
        txtPersonalInfo.setRows(5);
        jScrollPane1.setViewportView(txtPersonalInfo);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 170, 450, 130));

        txtPregnancyInfo.setColumns(20);
        txtPregnancyInfo.setRows(5);
        jScrollPane2.setViewportView(txtPregnancyInfo);

        jPanel1.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 350, 450, 118));

        tblVisitHistory.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane3.setViewportView(tblVisitHistory);

        jPanel1.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 550, 870, 170));

        btnBack.setText("Back");
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });
        jPanel1.add(btnBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(1020, 670, 163, 42));

        jLabel1.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Personal Information");
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 140, -1, -1));

        jLabel2.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Prenancy Information");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 320, -1, -1));

        jLabel3.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Medical History");
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 520, -1, -1));

        txtCheckupDetails.setColumns(20);
        txtCheckupDetails.setRows(5);
        jScrollPane4.setViewportView(txtCheckupDetails);

        jPanel1.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(617, 120, 490, 185));

        jLabel4.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("CheckUp Details");
        jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 90, -1, -1));

        jLabel5.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Medical History");
        jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 320, -1, -1));

        txtMedicalHistory.setColumns(20);
        txtMedicalHistory.setRows(5);
        jScrollPane5.setViewportView(txtMedicalHistory);

        jPanel1.add(jScrollPane5, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 350, 490, 148));

        jLabel6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pr_system1/10.jpg"))); // NOI18N
        jPanel1.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(-490, 0, 1710, 760));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        // TODO add your handling code here:
        PatientUI patientWindow = new PatientUI();
        patientWindow.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnBackActionPerformed


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

        java.awt.EventQueue.invokeLater(() -> {
            new ViewPatientUI(1).setVisible(true); // sample ID for testing
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JLabel lblName;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JTable tblVisitHistory;
    private javax.swing.JTextArea txtCheckupDetails;
    private javax.swing.JTextArea txtMedicalHistory;
    private javax.swing.JTextArea txtPersonalInfo;
    private javax.swing.JTextArea txtPregnancyInfo;
    // End of variables declaration//GEN-END:variables
}
