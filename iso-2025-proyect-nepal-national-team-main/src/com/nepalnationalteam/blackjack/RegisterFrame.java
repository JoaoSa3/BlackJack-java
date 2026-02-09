package com.nepalnationalteam.blackjack;

import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class RegisterFrame extends JFrame {
    private final JTextField txtEmail = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JPasswordField txtConfirm = new JPasswordField();
    private final JTextField txtDisplayName = new JTextField();
    private final JTextField txtId = new JTextField();

    private final JRadioButton rbPassport = new JRadioButton("Passport");
    private final JRadioButton rbNIE = new JRadioButton("Worker ID (NIE)");
    private final JRadioButton rbDNI = new JRadioButton("Normal ID (DNI)");

    private final JCheckBox chkShow = new JCheckBox("Show passwords");
    private final FirebaseService firebase = new FirebaseService();

    public RegisterFrame(StartScreenFrame start) {
        setTitle("Register");
        setSize(520, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);
        setLayout(null);

        JLabel lblEmail = mk("Email", 50, 30, 150, 24); add(lblEmail);
        txtEmail.setBounds(50, 54, 360, 28); add(txtEmail);

        JLabel lblName = mk("Display name", 50, 90, 150, 24); add(lblName);
        txtDisplayName.setBounds(50, 114, 360, 28); add(txtDisplayName);

        JLabel lblPass = mk("Password", 50, 150, 150, 24); add(lblPass);
        txtPassword.setBounds(50, 174, 360, 28); txtPassword.setEchoChar('•'); add(txtPassword);

        JLabel lblConf = mk("Confirm password", 50, 210, 180, 24); add(lblConf);
        txtConfirm.setBounds(50, 234, 360, 28); txtConfirm.setEchoChar('•'); add(txtConfirm);

        chkShow.setBounds(50, 270, 200, 20); chkShow.setForeground(Color.WHITE); chkShow.setOpaque(false); add(chkShow);
        chkShow.addActionListener(e -> {
            char echo = chkShow.isSelected() ? (char)0 : '•';
            txtPassword.setEchoChar(echo);
            txtConfirm.setEchoChar(echo);
        });

        JLabel lblIds = mk("Select ID", 50, 300, 150, 24); add(lblIds);
        ButtonGroup group = new ButtonGroup();
        for (JRadioButton rb : new JRadioButton[]{rbPassport, rbNIE, rbDNI}) {
            rb.setForeground(Color.WHITE);
            rb.setOpaque(false);
            group.add(rb);
            add(rb);
        }
        rbPassport.setBounds(50, 325, 150, 20);
        rbNIE.setBounds(200, 325, 150, 20);
        rbDNI.setBounds(350, 325, 150, 20);
        rbPassport.setSelected(true);

        JLabel lblIdVal = mk("ID value", 50, 355, 150, 24); add(lblIdVal);
        txtId.setBounds(50, 379, 360, 28); add(txtId);

        JButton btnCreate = ui("Create account"); btnCreate.setBounds(50, 430, 180, 40); add(btnCreate);
        JButton btnBack = ui("Back"); btnBack.setBounds(250, 430, 160, 40); add(btnBack);

        btnBack.addActionListener(e -> { dispose(); new StartScreenFrame().setVisible(true); });
        btnCreate.addActionListener(e -> doRegister());
    }

    private JLabel mk(String t, int x, int y, int w, int h) { JLabel l = new JLabel(t); l.setForeground(Color.WHITE); l.setBounds(x,y,w,h); return l; }
    private JButton ui(String t){ JButton b=new JButton(t); b.setBackground(new Color(30,30,30)); b.setForeground(new Color(220,220,220)); b.setFocusPainted(false); b.setFont(new Font("Segoe UI", Font.BOLD, 14)); b.setBorder(BorderFactory.createLineBorder(new Color(70,70,70))); return b; }

    private String selectedType(){ if(rbPassport.isSelected()) return "PASSPORT"; if(rbNIE.isSelected()) return "WORKER_NIE"; return "NORMAL_DNI"; }

    private boolean validatePassport(String v){ if(v==null) return false; v=v.trim().toUpperCase(); return v.matches("[A-Z0-9]{6,9}"); }
    private boolean validateDNI(String v){ if(v==null) return false; v=v.trim().toUpperCase(); if(!v.matches("\\\\d{8}[A-Z]")) return false; String letters="TRWAGMYFPDXBNJZSQVHLCKE"; int num=Integer.parseInt(v.substring(0,8)); char expected=letters.charAt(num%23); return v.charAt(8)==expected; }
    private boolean validateNIE(String v){ if(v==null) return false; v=v.trim().toUpperCase(); if(!v.matches("[XYZ]\\\\d{7}[A-Z]")) return false; String letters="TRWAGMYFPDXBNJZSQVHLCKE"; char first=v.charAt(0); int prefix=(first=='X')?0:(first=='Y'?1:2); int num=Integer.parseInt(prefix + v.substring(1,8)); char expected=letters.charAt(num%23); return v.charAt(8)==expected; }

    private void doRegister() {
        String email = txtEmail.getText().trim();
        String name = txtDisplayName.getText().trim();
        String p1 = new String(txtPassword.getPassword());
        String p2 = new String(txtConfirm.getPassword());
        String idType = selectedType();
        String idValue = txtId.getText().trim().toUpperCase();

        if (email.isEmpty() || name.isEmpty() || p1.isEmpty() || p2.isEmpty() || idValue.isEmpty()) { JOptionPane.showMessageDialog(this,"All fields are required."); return; }
        if (!p1.equals(p2)) { JOptionPane.showMessageDialog(this,"Passwords do not match."); return; }

        boolean ok = "PASSPORT".equals(idType) ? validatePassport(idValue)
                   : "WORKER_NIE".equals(idType) ? validateNIE(idValue)
                   : validateDNI(idValue);
        if (!ok) { JOptionPane.showMessageDialog(this,"Invalid ID format or control character."); return; }

        try {
            FirebaseService.AuthResult r = firebase.signUp(email, p1, name);

            if (FirebaseService.DATABASE_URL != null && FirebaseService.DATABASE_URL.startsWith("http")) {
                String userPath = FirebaseService.DATABASE_URL + "users/" + r.localId + ".json?auth=" + r.idToken;
                String body = "{\"email\":\"" + js(email) + "\","
                            + "\"displayName\":\"" + js(name) + "\","
                            + "\"selectedId\":\"" + idType + "\","
                            + "\"idType\":\"" + idType + "\","
                            + "\"idValue\":\"" + js(idValue) + "\"}";
                URL url = new URL(userPath);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                byte[] out = body.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(out.length);
                conn.connect();
                try (OutputStream os = conn.getOutputStream()) { os.write(out); }
                int code = conn.getResponseCode();
                if (code >= 400) {
                    String resp = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                    throw new RuntimeException("Profile write failed: HTTP " + code + " " + resp);
                }
            }

            SessionManager.Session s = new SessionManager.Session();
            s.email = r.email; s.displayName = name; s.idToken = r.idToken; s.refreshToken = r.refreshToken; s.localId = r.localId;
            s.selectedId = idType; s.idType = idType; s.idValue = idValue; s.rememberMe = true;
            SessionManager.save(s);

            JOptionPane.showMessageDialog(this,"Account created. Welcome " + name + "!");
            dispose(); new LobbyFrame(s.email, s.displayName).setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,"Register failed: " + ex.getMessage());
        }
    }

    private static String js(String s){ if(s==null) return ""; return s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
