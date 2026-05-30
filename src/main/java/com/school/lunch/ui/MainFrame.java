package com.school.lunch.ui;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.school.lunch.model.ActiveDate;
import com.school.lunch.model.Assignment;
import com.school.lunch.model.Teacher;
import com.school.lunch.service.AssignerService;
import com.school.lunch.util.ExcelUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainFrame extends JFrame {
    private JTabbedPane tabbedPane;
    private List<Teacher> teachers = new ArrayList<>();
    private List<ActiveDate> activeDates = new ArrayList<>();
    private List<Assignment> finalResults = new ArrayList<>();
    private AssignerService assignerService = new AssignerService();
    private javax.swing.border.TitledBorder locationTitleBorder;

    // Menu Items
    private JMenuItem step1Item;
    private JMenuItem step2Item;
    private JMenuItem step3Item;

    // UI Components
    private TeacherTableModel teacherTableModel;
    private DateTableModel dateTableModel;
    private ResultTableModel resultTableModel;
    private StatTableModel statTableModel;

    private JTable teacherTable;
    private JTable dateTable;
    private JTable resultTable;
    private JTable statTable;

    public MainFrame() {
        setTitle("급식지도 자동 배치 시스템");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 상용 프로그램 느낌의 아이콘 설정
        try {
            java.net.URL imgURL = getClass().getResource("/icon.png");
            if (imgURL != null) {
                ImageIcon icon = new ImageIcon(imgURL);
                setIconImage(icon.getImage());
            }
        } catch (Exception e) {
            // 아이콘 로드 실패 시 무시
            e.printStackTrace();
        }

        // 상단 메뉴바 설정
        setJMenuBar(createMenuBar());

        // UI 전역 설정 (테이블 디자인)
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", true);
        UIManager.put("Table.alternateRowColor", new Color(245, 247, 250));

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tabPlacement, int runCount, int maxTabHeight) {
                return 0;
            }
            @Override
            protected int calculateTabAreaWidth(int tabPlacement, int runCount, int maxTabWidth) {
                return 0;
            }
        });
        
        tabbedPane.addTab("1단계", createStep1Panel());
        tabbedPane.addTab("2단계", createStep2Panel());
        tabbedPane.addTab("3단계", createStep3Panel());

        tabbedPane.setEnabledAt(1, false);
        tabbedPane.setEnabledAt(2, false);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPane.add(tabbedPane, BorderLayout.CENTER);
        
        setContentPane(contentPane);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("도움말 (Help)");
        helpMenu.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));

        JMenuItem howToItem = new JMenuItem("사용 방법 안내");
        howToItem.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        howToItem.addActionListener(e -> showHowToDialog());

        JMenuItem aboutItem = new JMenuItem("프로그램 정보");
        aboutItem.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        aboutItem.addActionListener(e -> showAboutDialog());

        JMenu stepMenu = new JMenu("급식배치 단계");
        stepMenu.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));

        step1Item = new JMenuItem("1단계(지도교사 확정)");
        step1Item.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        step1Item.addActionListener(e -> selectStep(0));

        step2Item = new JMenuItem("2단계(장소 및 배식일 확정)");
        step2Item.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        step2Item.addActionListener(e -> selectStep(1));
        step2Item.setEnabled(false);

        step3Item = new JMenuItem("3단계(배치결과보기)");
        step3Item.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        step3Item.addActionListener(e -> selectStep(2));
        step3Item.setEnabled(false);

        stepMenu.add(step1Item);
        stepMenu.add(step2Item);
        stepMenu.add(step3Item);
        menuBar.add(stepMenu);

        helpMenu.add(howToItem);
        helpMenu.addSeparator();
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private void selectStep(int index) {
        if (!tabbedPane.isEnabledAt(index)) {
            String stepName = index == 1 ? "2단계" : index == 2 ? "3단계" : "1단계";
            JOptionPane.showMessageDialog(this, stepName + "는 먼저 이전 단계를 완료해야 합니다.", "안내", JOptionPane.INFORMATION_MESSAGE);
            if (index == 1) tabbedPane.setSelectedIndex(1);
            else if (index == 2) tabbedPane.setSelectedIndex(2);
            return;
        }
        if (index == 2) {
            if (finalResults.isEmpty()) {
                JOptionPane.showMessageDialog(this, "먼저 2단계에서 최종 배치를 완료해주세요.", "안내", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        tabbedPane.setSelectedIndex(index);
    }

    private void showHowToDialog() {
        String msg = "<html>" +
                "<body style='font-family: \"Malgun Gothic\", sans-serif; width: 480px;'>" +
                "    <h2 style='color: #1E40AF; margin: 0 0 8px 0;'>🍽️ 급식지도 자동 배치 시스템 사용 안내</h2>" +
                "    <p style='color: #475569; font-size: 13px; margin: 0 0 15px 0;'>" +
                "        교사 시간표 데이터를 기반으로 편차 없고 공정한 급식 지도 배정을 3단계로 완수합니다." +
                "    </p>" +
                "    " +
                "    <div style='margin-bottom: 12px;'>" +
                "        <b style='color: #2563EB; font-size: 14px;'>1단계: 교사 명단 확정</b>" +
                "        <ul style='margin: 4px 0; padding-left: 20px; font-size: 12px; color: #334155;'>" +
                "            <li><b>시간표 업로드:</b> 상단의 점선 영역을 클릭하여 교사들의 4교시 수업 정보가 담긴 엑셀(.xlsx) 파일을 업로드합니다.</li>" +
                "            <li><b>참여 교사 선택:</b> 표의 첫 번째 열 체크박스를 활용해 실제 급식 지도 대상이 되는 교사만 활성화합니다. (미참여 교사는 해제)</li>" +
                "            <li>확정 후 우측 하단의 [교사 확정 및 기간 설정 이동] 버튼을 클릭합니다.</li>" +
                "        </ul>" +
                "    </div>" +
                "    " +
                "    <div style='margin-bottom: 12px;'>" +
                "        <b style='color: #2563EB; font-size: 14px;'>2단계: 장소 및 기간 확정</b>" +
                "        <ul style='margin: 4px 0; padding-left: 20px; font-size: 12px; color: #334155;'>" +
                "            <li><b>지도 장소 설정 (동적 CRUD):</b> 좌측 패널에서 지도 장소를 <b>[추가], [수정], [삭제]</b>할 수 있습니다. 장소 개수에 맞춰 공정한 순환 배치가 이뤄집니다.</li>" +
                "            <li><b>지도 기간 및 목록 생성:</b> 우측 패널에서 지도를 수행할 시작일과 종료일을 설정한 후 <b>[배식 가능 날짜 목록 생성]</b> 버튼을 누릅니다.</li>" +
                "            <li><b>제외일 설정:</b> 하단 일자 목록에서 재량휴업일, 개교기념일 등 배식이 없는 날은 체크박스를 해제합니다.</li>" +
                "            <li><b>배치 시작:</b> [최종 배치하기]를 누르면 참여 교사 수와 장소 개수 적합성을 검증한 뒤 배치 로직이 작동합니다.</li>" +
                "        </ul>" +
                "    </div>" +
                "    " +
                "    <div style='margin-bottom: 12px;'>" +
                "        <b style='color: #2563EB; font-size: 14px;'>3단계: 최종 배치 및 저장</b>" +
                "        <ul style='margin: 4px 0; padding-left: 20px; font-size: 12px; color: #334155;'>" +
                "            <li><b>결과 분석:</b> <b>[일자별 배치표]</b>와 <b>[교사별 배정 횟수 통계]</b>를 실시간 갱신형 뷰로 대조 확인합니다.</li>" +
                "            <li><b>결과 저장:</b> [엑셀 파일 저장]을 클릭하여 가독성이 뛰어난 전용 엑셀 보고서 형태로 최종 산출물을 저장합니다.</li>" +
                "        </ul>" +
                "    </div>" +
                "    " +
                "    <div style='background-color: #EFF6FF; border-left: 4px solid #3B82F6; padding: 8px 12px; font-size: 12px; color: #1E3A8A; margin-top: 15px;'>" +
                "        <b>💡 AI 최적화 배치 팁:</b><br>" +
                "        교사별 4교시 수업 유무를 최우선으로 고려하며, 특정 장소에 배정이 쏠리지 않도록 누적 횟수 편차를 제로(0)에 가깝게 최소화합니다." +
                "    </div>" +
                "</body>" +
                "</html>";
        JOptionPane.showMessageDialog(this, msg, "사용 방법 안내", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAboutDialog() {
        String msg = "<html>" +
                "<body style='font-family: \"Malgun Gothic\", sans-serif; width: 440px;'>" +
                "    <div style='text-align: center; margin-bottom: 15px;'>" +
                "        <h2 style='color: #1E40AF; margin: 0; font-size: 20px;'>🏫 급식지도 자동 배치 시스템</h2>" +
                "        <span style='color: #64748B; font-size: 12px; font-weight: bold;'>Lunch Assignment System v1.0.0 (Stable)</span>" +
                "    </div>" +
                "    " +
                "    <p style='color: #334155; font-size: 13px; line-height: 1.6; margin: 0 0 15px 0; text-align: justify;'>" +
                "        본 프로그램은 교직원의 수업 정보 및 학사 일정을 종합 분석하여,<br> " +
                "        급식 지도 배정 업무의 편차를 줄이고 형평성 있는 공정 배치를 실현하는<br> " +
                "        <b>학교 행정 업무 자동화 솔루션</b>입니다." +
                "    </p>" +
                "    " +
                "    <div style='background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 6px; padding: 12px; margin-bottom: 15px;'>" +
                "        <b style='color: #1E293B; font-size: 13px;'>주요 시스템 사양 및 기능</b>" +
                "        <table style='width: 100%; font-size: 11px; color: #475569; margin-top: 6px; border-collapse: collapse;'>" +
                "            <tr style='border-bottom: 1px solid #F1F5F9;'>" +
                "                <td style='padding: 5px 0; font-weight: bold; width: 90px; color: #1E40AF;'>배정 알고리즘</td>" +
                "                <td style='padding: 5px 0;'>교사별 누적 배정 횟수 편차 최소화 분배 엔진</td>" +
                "            </tr>" +
                "            <tr style='border-bottom: 1px solid #F1F5F9;'>" +
                "                <td style='padding: 5px 0; font-weight: bold; color: #1E40AF;'>장소 동적 관리</td>" +
                "                <td style='padding: 5px 0;'>실시간 장소 추가/수정/삭제 및 타이틀 갯수 연동</td>" +
                "            </tr>" +
                "            <tr style='border-bottom: 1px solid #F1F5F9;'>" +
                "                <td style='padding: 5px 0; font-weight: bold; color: #1E40AF;'>2단 레이아웃</td>" +
                "                <td style='padding: 5px 0;'>가시성을 극대화한 2단 배식일 목록 편집 테이블</td>" +
                "            </tr>" +
                "            <tr>" +
                "                <td style='padding: 5px 0; font-weight: bold; color: #1E40AF;'>엑셀 내보내기</td>" +
                "                <td style='padding: 5px 0;'>가운데 정렬, 격행 그라데이션 및 1단 합계 강조 서식</td>" +
                "            </tr>" +
                "        </table>" +
                "    </div>" +
                "    " +
                "    <div style='font-size: 11px; color: #64748B; text-align: center; border-top: 1px solid #F1F5F9; padding-top: 10px; line-height: 1.5;'>" +
                "        <b>제작자: 청죽 </b><br>" +
                "        <b>이메일: jptr94@gmail.com</b><br>" +
                "        <span style='display: inline-block; margin-top: 5px; color: #94A3B8;'>© 2026 한국의 푸르른 대나무. All rights reserved.</span>" +
                "    </div>" +
                "</body>" +
                "</html>";
        JOptionPane.showMessageDialog(this, msg, "프로그램 정보", JOptionPane.INFORMATION_MESSAGE);
    }

    private void styleTable(JTable table) {
        table.getTableHeader().setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(230, 235, 240));
        table.setSelectionBackground(new Color(180, 210, 255));
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for(int i=0; i<table.getColumnCount(); i++) {
            if(table.getColumnClass(i) != Boolean.class) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }
    }

    private JPanel createHeaderPanel(String title, String subtitle) {
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        titleLabel.setForeground(new Color(30, 64, 175)); // Blue-800
        
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(100, 116, 139)); // Slate-500
        
        header.add(titleLabel);
        header.add(subtitleLabel);
        return header;
    }

    private JPanel createStep1Panel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        panel.add(createHeaderPanel("1단계: 시간표 업로드 및 지도교사 확정", "엑셀 시간표를 분석하여 공정하게 지도교사를 배치합니다."), BorderLayout.NORTH);

        teacherTableModel = new TeacherTableModel();
        teacherTable = new JTable(teacherTableModel);
        styleTable(teacherTable);

        // Custom Cell Renderer for Lessons
        DefaultTableCellRenderer lessonRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                if (!isSelected) {
                    if (value == null || value.toString().isEmpty()) {
                        c.setBackground(new Color(250, 250, 180)); // Light Yellow
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        };
        for(int i=3; i<8; i++) {
            teacherTable.getColumnModel().getColumn(i).setCellRenderer(lessonRenderer);
        }

        // Upload Button Panel (Dashed line effect)
        JButton uploadBtn = new JButton("<html><div style='text-align: center;'><span style='font-size: 24px;'>☁</span><br>클릭하여 교사시간표(xlsx) 업로드</div></html>");
        uploadBtn.setBackground(Color.WHITE);
        uploadBtn.setForeground(new Color(100, 116, 139));
        uploadBtn.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        uploadBtn.setFocusPainted(false);
        uploadBtn.setPreferredSize(new Dimension(800, 100));
        uploadBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(new Color(156, 163, 175), 2, 5, 2, false),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        topPanel.add(uploadBtn);

        // Status bar & Table
        JPanel tableHeaderPanel = new JPanel(new BorderLayout());
        
        // 좌측: 제목과 체크박스
        JPanel leftHeaderWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JLabel tableTitle = new JLabel("교사 명단 및 4교시 수업 정보 확인 (급식지도 대상자만 체크)");
        tableTitle.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        
        JCheckBox selectAllCheckBox = new JCheckBox("전체");
        selectAllCheckBox.setFont(new Font("Malgun Gothic", Font.PLAIN, 11));
        selectAllCheckBox.setFocusPainted(false);
        leftHeaderWrap.add(tableTitle);
        leftHeaderWrap.add(selectAllCheckBox);
        
        // 우측: 선택 인원 표시
        JLabel selectedCountLabel = new JLabel("선택된 인원: 0명");
        selectedCountLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        selectedCountLabel.setForeground(new Color(37, 99, 235));
        
        tableHeaderPanel.add(leftHeaderWrap, BorderLayout.WEST);
        tableHeaderPanel.add(selectedCountLabel, BorderLayout.EAST);
        tableHeaderPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(topPanel, BorderLayout.NORTH);
        
        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.add(tableHeaderPanel, BorderLayout.NORTH);
        tableWrap.add(new JScrollPane(teacherTable), BorderLayout.CENTER);
        
        centerPanel.add(tableWrap, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
        JLabel bottomStatusLabel = new JLabel("총 0명의 교사가 선택되었습니다.");
        bottomStatusLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        
        JButton nextBtn = new JButton("교사 확정");
        nextBtn.setBackground(new Color(37, 99, 235));
        nextBtn.setForeground(Color.WHITE);
        nextBtn.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        nextBtn.setPreferredSize(new Dimension(220, 40));

        bottomPanel.add(bottomStatusLabel, BorderLayout.WEST);
        bottomPanel.add(nextBtn, BorderLayout.EAST);

        Runnable updateCountTask = () -> {
            long count = teachers.stream().filter(Teacher::isSelected).count();
            selectAllCheckBox.setSelected(count == teachers.size() && teachers.size() > 0);
            selectedCountLabel.setText("선택된 인원: " + count + "명");
            bottomStatusLabel.setText("총 " + count + "명의 교사가 선택되었습니다.");
        };

        uploadBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter("엑셀 파일 (*.xlsx)", "xlsx");
            fileChooser.setFileFilter(filter);
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    teachers = ExcelUtil.readTeachersFromExcel(fileChooser.getSelectedFile());
                    teacherTableModel.fireTableDataChanged();
                    updateCountTask.run();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "파일 읽기 오류: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        teacherTableModel.addTableModelListener(e -> updateCountTask.run());

        selectAllCheckBox.addActionListener(e -> {
            boolean isSelected = selectAllCheckBox.isSelected();
            for (Teacher t : teachers) {
                t.setSelected(isSelected);
            }
            teacherTableModel.fireTableDataChanged();
            updateCountTask.run();
        });

        nextBtn.addActionListener(e -> {
            long selectedCount = teachers.stream().filter(Teacher::isSelected).count();
            if (selectedCount == 0) {
                JOptionPane.showMessageDialog(this, "급식 지도에 참여할 교사를 최소 1명 이상 선택해야 합니다.", "선택 확인", JOptionPane.WARNING_MESSAGE);
                return;
            }
            tabbedPane.setEnabledAt(1, true);
            step2Item.setEnabled(true);
            JOptionPane.showMessageDialog(this, "지도교사 확정되었습니다.", "확정 완료", JOptionPane.INFORMATION_MESSAGE);
            tabbedPane.setSelectedIndex(1);
        });

        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createStep2Panel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        panel.add(createHeaderPanel("2단계: 급식지도 장소, 기간 및 배식일 확정", "장소를 설정하고 시작일과 종료일을 지정한 후, 재량휴업일 등 배식이 없는 날을 체크 해제하세요."), BorderLayout.NORTH);

        // 상단 가로 2분할 설정 패널
        JPanel topSettingsPanel = new JPanel(new GridLayout(1, 2, 20, 0));

        // 1. 좌측 패널: 지도 기간 설정
        JPanel leftDatePanel = new JPanel(new BorderLayout(10, 10));
        leftDatePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(218, 224, 233)), 
                "지도 기간 설정", 
                javax.swing.border.TitledBorder.LEFT, 
                javax.swing.border.TitledBorder.TOP, 
                new Font("Malgun Gothic", Font.BOLD, 14), 
                new Color(37, 99, 235)
        ));
        leftDatePanel.setBorder(BorderFactory.createCompoundBorder(
                leftDatePanel.getBorder(),
                new EmptyBorder(10, 15, 10, 15)
        ));

        JPanel datePickersPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        
        JPanel startPanel = new JPanel(new BorderLayout(0, 5));
        startPanel.add(new JLabel("지도 시작일"), BorderLayout.NORTH);
        DatePickerSettings startSettings = new DatePickerSettings();
        startSettings.setFormatForDatesCommonEra("yyyy-MM-dd");
        DatePicker startDatePicker = new DatePicker(startSettings);
        startPanel.add(startDatePicker, BorderLayout.CENTER);

        JPanel endPanel = new JPanel(new BorderLayout(0, 5));
        endPanel.add(new JLabel("지도 종료일"), BorderLayout.NORTH);
        DatePickerSettings endSettings = new DatePickerSettings();
        endSettings.setFormatForDatesCommonEra("yyyy-MM-dd");
        DatePicker endDatePicker = new DatePicker(endSettings);
        endPanel.add(endDatePicker, BorderLayout.CENTER);

        datePickersPanel.add(startPanel);
        datePickersPanel.add(endPanel);

        leftDatePanel.add(datePickersPanel, BorderLayout.CENTER);

        JButton generateBtn = new JButton("배식 가능 날짜 목록 생성");
        generateBtn.setBackground(new Color(254, 224, 71)); // Beautiful yellow (#FEE047)
        generateBtn.setForeground(new Color(69, 26, 3)); // Dark brown text for high premium contrast
        generateBtn.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        generateBtn.setPreferredSize(new Dimension(240, 35));
        
        JPanel generateBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        generateBtnPanel.add(generateBtn);
        leftDatePanel.add(generateBtnPanel, BorderLayout.SOUTH);

        // 2. 우측 패널: 급식 지도 장소 설정
        JPanel rightLocationPanel = new JPanel(new BorderLayout(10, 10));
        locationTitleBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(218, 224, 233)), 
                "급식 지도 장소 설정 (" + assignerService.getPlaces().size() + "곳)", 
                javax.swing.border.TitledBorder.LEFT, 
                javax.swing.border.TitledBorder.TOP, 
                new Font("Malgun Gothic", Font.BOLD, 14), 
                new Color(37, 99, 235)
        );
        rightLocationPanel.setBorder(BorderFactory.createCompoundBorder(
                locationTitleBorder,
                new EmptyBorder(10, 15, 10, 15)
        ));

        DefaultListModel<String> locationListModel = new DefaultListModel<>();
        for (String place : assignerService.getPlaces()) {
            locationListModel.addElement(place);
        }
        JList<String> locationList = new JList<>(locationListModel);
        locationList.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        locationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroller = new JScrollPane(locationList);
        listScroller.setPreferredSize(new Dimension(200, 80));

        // 버튼 패널
        JPanel locBtnGrid = new JPanel(new GridLayout(3, 1, 0, 5));
        JButton addLocBtn = new JButton("추가");
        JButton editLocBtn = new JButton("수정");
        JButton deleteLocBtn = new JButton("삭제");
        
        addLocBtn.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        editLocBtn.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        deleteLocBtn.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));

        locBtnGrid.add(addLocBtn);
        locBtnGrid.add(editLocBtn);
        locBtnGrid.add(deleteLocBtn);

        JPanel btnWrapPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnWrapPanel.add(locBtnGrid);

        rightLocationPanel.add(listScroller, BorderLayout.CENTER);
        rightLocationPanel.add(btnWrapPanel, BorderLayout.EAST);

        topSettingsPanel.add(rightLocationPanel); // 장소 설정을 좌측에 배치
        topSettingsPanel.add(leftDatePanel);      // 기간 설정을 우측에 배치

        // topWrap 구성
        JPanel topWrap = new JPanel(new BorderLayout());
        topWrap.add(topSettingsPanel, BorderLayout.CENTER);
        topWrap.setBorder(new EmptyBorder(0, 0, 15, 0));

        JSeparator divider = new JSeparator();
        topWrap.add(divider, BorderLayout.SOUTH);

        // Table Title and Badge
        JPanel tableHeaderPanel = new JPanel(new BorderLayout());
        tableHeaderPanel.setBorder(new EmptyBorder(15, 0, 10, 0));
        
        JPanel titleWrap = new JPanel(new GridLayout(2, 1));
        JLabel tableTitle = new JLabel("배식 활동 대상일 선택 (공휴일 등 제외)");
        tableTitle.setFont(new Font("Malgun Gothic", Font.BOLD, 15));
        JLabel tableSubtitle = new JLabel("* 배식 지도가 있는 날만 체크하세요.");
        tableSubtitle.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        tableSubtitle.setForeground(new Color(100, 116, 139));
        titleWrap.add(tableTitle);
        titleWrap.add(tableSubtitle);
        
        // 좌측: 제목과 체크박스
        JPanel leftHeaderWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JCheckBox dateSelectAllCheckBox = new JCheckBox("전체");
        dateSelectAllCheckBox.setFont(new Font("Malgun Gothic", Font.PLAIN, 11));
        dateSelectAllCheckBox.setFocusPainted(false);
        leftHeaderWrap.add(titleWrap);
        leftHeaderWrap.add(dateSelectAllCheckBox);
        
        JLabel selectedDateCountLabel = new JLabel("선택된 배식일: 0일", SwingConstants.CENTER);
        selectedDateCountLabel.setOpaque(true);
        selectedDateCountLabel.setBackground(new Color(220, 252, 231)); // Light green background
        selectedDateCountLabel.setForeground(new Color(22, 163, 74)); // Dark green text
        selectedDateCountLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        selectedDateCountLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(134, 239, 172), 1, true),
                new EmptyBorder(5, 15, 5, 15)
        ));

        tableHeaderPanel.add(leftHeaderWrap, BorderLayout.WEST);
        tableHeaderPanel.add(selectedDateCountLabel, BorderLayout.EAST);

        dateTableModel = new DateTableModel();
        dateTable = new JTable(dateTableModel);
        styleTable(dateTable);
        
        // 2단 컬럼 너비 설정 (선택, 날짜, 요일, 선택, 날짜, 요일)
        if (dateTable.getColumnCount() == 6) {
            dateTable.getColumnModel().getColumn(0).setPreferredWidth(50);
            dateTable.getColumnModel().getColumn(1).setPreferredWidth(120);
            dateTable.getColumnModel().getColumn(2).setPreferredWidth(80);
            dateTable.getColumnModel().getColumn(3).setPreferredWidth(50);
            dateTable.getColumnModel().getColumn(4).setPreferredWidth(120);
            dateTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        }

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(topWrap, BorderLayout.NORTH);
        
        JPanel tableWrapPanel = new JPanel(new BorderLayout());
        tableWrapPanel.add(tableHeaderPanel, BorderLayout.NORTH);
        tableWrapPanel.add(new JScrollPane(dateTable), BorderLayout.CENTER);
        centerPanel.add(tableWrapPanel, BorderLayout.CENTER);

        // Bottom section: 버튼만 간결하게
        JButton runBtn = new JButton("최종 배치하기");
        runBtn.setBackground(new Color(22, 163, 74)); // Green-600
        runBtn.setForeground(Color.WHITE);
        runBtn.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        runBtn.setPreferredSize(new Dimension(200, 42));
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        bottomPanel.add(runBtn);

        // Logic
        Runnable updateDateCountTask = () -> {
            long count = activeDates.stream().filter(ActiveDate::isChecked).count();
            dateSelectAllCheckBox.setSelected(count == activeDates.size() && activeDates.size() > 0);
            selectedDateCountLabel.setText("선택된 배식일: " + count + "일");
        };

        generateBtn.addActionListener(e -> {
            LocalDate start = startDatePicker.getDate();
            LocalDate end = endDatePicker.getDate();
            
            if(start == null || end == null) {
                JOptionPane.showMessageDialog(this, "시작일과 종료일을 모두 선택해주세요.", "안내", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if(start.isAfter(end)) {
                JOptionPane.showMessageDialog(this, "종료일이 시작일보다 빠를 수 없습니다.", "안내", JOptionPane.WARNING_MESSAGE);
                return;
            }

            activeDates.clear();
            LocalDate current = start;
            while (!current.isAfter(end)) {
                DayOfWeek dow = current.getDayOfWeek();
                if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                    String dayStr = dow.getDisplayName(TextStyle.SHORT, Locale.KOREAN);
                    int dayIdx = dow.getValue();
                    activeDates.add(new ActiveDate(current, dayStr, dayIdx));
                }
                current = current.plusDays(1);
            }
            dateTableModel.fireTableDataChanged();
            updateDateCountTask.run();
        });

        dateTableModel.addTableModelListener(e -> updateDateCountTask.run());

        dateSelectAllCheckBox.addActionListener(e -> {
            boolean isSelected = dateSelectAllCheckBox.isSelected();
            for (ActiveDate d : activeDates) {
                d.setChecked(isSelected);
            }
            dateTableModel.fireTableDataChanged();
            updateDateCountTask.run();
        });

        // 추가 버튼 리스너
        addLocBtn.addActionListener(e -> {
            String newLoc = JOptionPane.showInputDialog(this, "새로운 급식 지도 장소 이름을 입력하세요:", "장소 추가", JOptionPane.QUESTION_MESSAGE);
            if (newLoc != null) {
                newLoc = newLoc.trim();
                if (newLoc.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "장소 이름은 비어있을 수 없습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (locationListModel.contains(newLoc)) {
                    JOptionPane.showMessageDialog(this, "이미 존재하는 장소 이름입니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                locationListModel.addElement(newLoc);
                syncPlaces(locationListModel);
            }
        });

        // 수정 버튼 리스너
        editLocBtn.addActionListener(e -> {
            int selectedIdx = locationList.getSelectedIndex();
            if (selectedIdx == -1) {
                JOptionPane.showMessageDialog(this, "수정할 장소를 선택해주세요.", "안내", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String oldLoc = locationListModel.getElementAt(selectedIdx);
            String newLoc = (String) JOptionPane.showInputDialog(this, "장소 이름을 수정하세요:", "장소 수정", JOptionPane.QUESTION_MESSAGE, null, null, oldLoc);
            if (newLoc != null) {
                newLoc = newLoc.trim();
                if (newLoc.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "장소 이름은 비어있을 수 없습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (!oldLoc.equals(newLoc) && locationListModel.contains(newLoc)) {
                    JOptionPane.showMessageDialog(this, "이미 존재하는 장소 이름입니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                locationListModel.set(selectedIdx, newLoc);
                syncPlaces(locationListModel);
            }
        });

        // 삭제 버튼 리스너
        deleteLocBtn.addActionListener(e -> {
            int selectedIdx = locationList.getSelectedIndex();
            if (selectedIdx == -1) {
                JOptionPane.showMessageDialog(this, "삭제할 장소를 선택해주세요.", "안내", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (locationListModel.size() <= 1) {
                JOptionPane.showMessageDialog(this, "최소 한 개 이상의 급식 지도 장소가 필요합니다.", "경고", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String loc = locationListModel.getElementAt(selectedIdx);
            int confirm = JOptionPane.showConfirmDialog(this, "'" + loc + "' 장소를 삭제하시겠습니까?", "장소 삭제 확인", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                locationListModel.remove(selectedIdx);
                syncPlaces(locationListModel);
            }
        });

        runBtn.addActionListener(e -> {
            List<ActiveDate> selectedDates = new ArrayList<>();
            for (ActiveDate d : activeDates) {
                if (d.isChecked()) selectedDates.add(d);
            }
            if (selectedDates.isEmpty()) {
                JOptionPane.showMessageDialog(this, "최소 하루 이상의 배식일을 선택해야 합니다.", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 인원 검증: 참여교사 수 >= 장소 개수
            int requiredTeachers = assignerService.getPlaces().size();
            long selectedCount = teachers.stream().filter(Teacher::isSelected).count();
            if (selectedCount < requiredTeachers) {
                JOptionPane.showMessageDialog(this, 
                        "급식 지도에 참여할 교사 수(" + selectedCount + "명)가 설정된 지도 장소 개수(" + requiredTeachers + "개)보다 적습니다.\n" +
                        "1단계에서 참여 교사를 더 선택하거나 2단계에서 지도 장소를 줄여주세요.", 
                        "인원 부족", JOptionPane.WARNING_MESSAGE);
                return;
            }

            finalResults = assignerService.runAssignment(teachers, selectedDates);
            System.out.println("[DEBUG] runAssignment completed, finalResults.size=" + finalResults.size());
            
            // 결과 테이블 헤더 및 스타일 갱신
            resultTableModel.updateHeaders();
            styleTable(resultTable);
            
            statTableModel.updateHeaders();
            styleTable(statTable);
            statTable.getColumnModel().getColumn(0).setPreferredWidth(120);
            
            tabbedPane.setEnabledAt(2, true);
            step3Item.setEnabled(true);
            JOptionPane.showMessageDialog(this, "최종 배치되었습니다.", "배치 완료", JOptionPane.INFORMATION_MESSAGE);
            tabbedPane.setSelectedIndex(2);
        });

        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createStep3Panel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel topWrap = new JPanel(new BorderLayout());
        topWrap.add(createHeaderPanel("3단계: 최종 배치 결과", "AI 최적화가 완료되었습니다. 장소별 횟수가 최대한 균등하게 배치되었습니다."), BorderLayout.CENTER);

        JButton exportBtn = new JButton("엑셀 파일 저장");
        exportBtn.setBackground(new Color(37, 99, 235));
        exportBtn.setForeground(Color.WHITE);
        exportBtn.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        exportBtn.setPreferredSize(new Dimension(170, 36));
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.add(exportBtn);
        topWrap.add(btnPanel, BorderLayout.EAST);

        resultTableModel = new ResultTableModel();
        resultTable = new JTable(resultTableModel);
        styleTable(resultTable);

        statTableModel = new StatTableModel();
        statTable = new JTable(statTableModel);
        styleTable(statTable);
        // 통계 테이블 컬럼 크기 약간 조절
        statTable.getColumnModel().getColumn(0).setPreferredWidth(120);

        JPanel tablePanel = new JPanel(new GridLayout(2, 1, 0, 15));
        
        JPanel resultWrap = new JPanel(new BorderLayout());
        JLabel l1 = new JLabel("일자별 배치표");
        l1.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        l1.setBorder(new EmptyBorder(0,0,5,0));
        resultWrap.add(l1, BorderLayout.NORTH);
        resultWrap.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        
        JPanel statWrap = new JPanel(new BorderLayout());
        JLabel l2 = new JLabel("교사별 배정 횟수 통계");
        l2.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        l2.setBorder(new EmptyBorder(0,0,5,0));
        statWrap.add(l2, BorderLayout.NORTH);
        statWrap.add(new JScrollPane(statTable), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, resultWrap, statWrap);
        splitPane.setDividerLocation(540);
        splitPane.setBorder(null);

        exportBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("엑셀 저장");
            javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter("엑셀 파일 (*.xlsx)", "xlsx");
            fileChooser.setFileFilter(filter);
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".xlsx")) {
                    file = new File(file.getParentFile(), file.getName() + ".xlsx");
                }
                try {
                    ExcelUtil.writeResultsToExcel(file, finalResults, assignerService.getPlaces(), assignerService.getTeacherCounts());
                    JOptionPane.showMessageDialog(this, "성공적으로 저장되었습니다.\n" + file.getAbsolutePath(), "저장 완료", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "저장 중 오류 발생: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        panel.add(topWrap, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    // --- Table Models ---
    private class TeacherTableModel extends AbstractTableModel {
        private String[] columnNames = {"선택", "순번", "교사명", "월(4)", "화(4)", "수(4)", "목(4)", "금(4)"};

        @Override public int getRowCount() { return teachers.size(); }
        @Override public int getColumnCount() { return columnNames.length; }
        @Override public String getColumnName(int column) { return columnNames[column]; }
        @Override public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }
        @Override public boolean isCellEditable(int rowIndex, int columnIndex) { return columnIndex == 0; }
        
        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            Teacher t = teachers.get(rowIndex);
            switch (columnIndex) {
                case 0: return t.isSelected();
                case 1: return rowIndex + 1;
                case 2: return t.getName();
                case 3: return t.getLessons()[0] != null ? t.getLessons()[0] : "";
                case 4: return t.getLessons()[1] != null ? t.getLessons()[1] : "";
                case 5: return t.getLessons()[2] != null ? t.getLessons()[2] : "";
                case 6: return t.getLessons()[3] != null ? t.getLessons()[3] : "";
                case 7: return t.getLessons()[4] != null ? t.getLessons()[4] : "";
            }
            return null;
        }

        @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                teachers.get(rowIndex).setSelected((Boolean) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

    private class DateTableModel extends AbstractTableModel {
        private String[] columnNames = {"선택", "날짜", "요일", "선택", "날짜", "요일"};

        @Override public int getRowCount() { 
            return (activeDates.size() + 1) / 2; 
        }
        @Override public int getColumnCount() { return columnNames.length; }
        @Override public String getColumnName(int column) { return columnNames[column]; }
        @Override public Class<?> getColumnClass(int columnIndex) {
            return (columnIndex == 0 || columnIndex == 3) ? Boolean.class : String.class;
        }
        @Override public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (columnIndex == 0) return true;
            if (columnIndex == 3) {
                int half = (activeDates.size() + 1) / 2;
                return (rowIndex + half) < activeDates.size();
            }
            return false;
        }

        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            int half = (activeDates.size() + 1) / 2;
            if (columnIndex < 3) {
                if (rowIndex < activeDates.size()) {
                    ActiveDate d = activeDates.get(rowIndex);
                    switch (columnIndex) {
                        case 0: return d.isChecked();
                        case 1: return d.getDate().toString();
                        case 2: return d.getDayOfWeek() + "요일";
                    }
                }
            } else {
                int targetIdx = rowIndex + half;
                if (targetIdx < activeDates.size()) {
                    ActiveDate d = activeDates.get(targetIdx);
                    switch (columnIndex - 3) {
                        case 0: return d.isChecked();
                        case 1: return d.getDate().toString();
                        case 2: return d.getDayOfWeek() + "요일";
                    }
                }
            }
            return "";
        }

        @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            int half = (activeDates.size() + 1) / 2;
            if (columnIndex == 0) {
                if (rowIndex < activeDates.size()) {
                    activeDates.get(rowIndex).setChecked((Boolean) aValue);
                    fireTableCellUpdated(rowIndex, columnIndex);
                }
            } else if (columnIndex == 3) {
                int targetIdx = rowIndex + half;
                if (targetIdx < activeDates.size()) {
                    activeDates.get(targetIdx).setChecked((Boolean) aValue);
                    fireTableCellUpdated(rowIndex, columnIndex);
                }
            }
        }
    }

    private class ResultTableModel extends AbstractTableModel {
        private List<String> columnNames = new ArrayList<>();

        public ResultTableModel() {
            updateHeaders();
        }

        public void updateHeaders() {
            columnNames.clear();
            columnNames.add("날짜");
            columnNames.add("요일");
            columnNames.addAll(assignerService.getPlaces());
            fireTableStructureChanged();
        }

        @Override public int getRowCount() { return finalResults.size(); }
        @Override public int getColumnCount() { return columnNames.size(); }
        @Override public String getColumnName(int column) { return columnNames.get(column); }

        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            Assignment a = finalResults.get(rowIndex);
            if (columnIndex == 0) return a.getDate().toString();
            if (columnIndex == 1) return a.getDayOfWeek();
            int placeIdx = columnIndex - 2;
            if (placeIdx >= 0 && placeIdx < a.getPlaces().length) {
                return a.getPlaces()[placeIdx];
            }
            return "";
        }
    }

    private class StatTableModel extends AbstractTableModel {
        private List<String> columnNames = new ArrayList<>();
        private List<Map.Entry<String, Map<String, Integer>>> statList = new ArrayList<>();

        public StatTableModel() {
            updateHeaders();
        }

        public void updateHeaders() {
            columnNames.clear();
            columnNames.add("교사명");
            columnNames.addAll(assignerService.getPlaces());
            columnNames.add("총 배정 횟수");
            statList = new ArrayList<>(assignerService.getTeacherCounts().entrySet());
            statList.sort((a, b) -> b.getValue().get("total").compareTo(a.getValue().get("total")));
            fireTableStructureChanged();
        }

        @Override public void fireTableDataChanged() {
            statList = new ArrayList<>(assignerService.getTeacherCounts().entrySet());
            statList.sort((a, b) -> b.getValue().get("total").compareTo(a.getValue().get("total")));
            super.fireTableDataChanged();
        }

        @Override public int getRowCount() { return statList.size(); }
        @Override public int getColumnCount() { return columnNames.size(); }
        @Override public String getColumnName(int column) { return columnNames.get(column); }

        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            Map.Entry<String, Map<String, Integer>> entry = statList.get(rowIndex);
            Map<String, Integer> counts = entry.getValue();
            if (columnIndex == 0) {
                return entry.getKey();
            } else if (columnIndex == columnNames.size() - 1) {
                return counts.get("total") + "회";
            } else {
                String place = columnNames.get(columnIndex);
                return counts.getOrDefault(place, 0);
            }
        }
    }


    private void syncPlaces(DefaultListModel<String> model) {
        List<String> updatedPlaces = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            updatedPlaces.add(model.getElementAt(i));
        }
        assignerService.setPlaces(updatedPlaces);
        if (locationTitleBorder != null) {
            locationTitleBorder.setTitle("급식 지도 장소 설정 (" + updatedPlaces.size() + "곳)");
            repaint();
        }
    }
}
