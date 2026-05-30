package com.school.lunch.service;

import com.school.lunch.model.ActiveDate;
import com.school.lunch.model.Assignment;
import com.school.lunch.model.Teacher;

import java.util.*;
import java.util.stream.Collectors;

public class AssignerService {
    private List<String> places = new ArrayList<>(Arrays.asList("정보내", "정보외", "별관내", "별관외"));
    
    // 통계 추적용 맵: 교사명 -> 장소별 횟수
    private Map<String, Map<String, Integer>> teacherCounts;

    public AssignerService() {
        this.teacherCounts = new HashMap<>();
    }

    public List<String> getPlaces() {
        return places;
    }

    public void setPlaces(List<String> places) {
        this.places = places;
    }

    public Map<String, Map<String, Integer>> getTeacherCounts() {
        return teacherCounts;
    }

    public void setTeacherCounts(Map<String, Map<String, Integer>> teacherCounts) {
        this.teacherCounts = teacherCounts;
    }

    public List<Assignment> runAssignment(List<Teacher> teachers, List<ActiveDate> selectedDates) {
        List<Assignment> results = new ArrayList<>();
        List<Teacher> pool = teachers.stream().filter(Teacher::isSelected).collect(Collectors.toList());

        // 통계 초기화
        teacherCounts.clear();
        for (Teacher t : pool) {
            Map<String, Integer> counts = new HashMap<>();
            counts.put("total", 0);
            for (String p : places) {
                counts.put(p, 0);
            }
            teacherCounts.put(t.getName(), counts);
        }

        Random rand = new Random();
        int n = places.size();

        for (int i = 0; i < selectedDates.size(); i++) {
            ActiveDate dateObj = selectedDates.get(i);
            int lessonIdx = dateObj.getDayIndex() - 1; // 1(월)~5(금) -> 0~4. JS에서는 1~5를 썼지만 자바에서 DayOfWeek 처리 필요. (1=월, 5=금)

            List<Teacher> available = new ArrayList<>();
            for (Teacher t : pool) {
                if (t.getLessons()[lessonIdx] == null) {
                    available.add(t);
                }
            }

            // 남은 미래 배식 가능일수 계산
            for (Teacher t : available) {
                int futureAvail = 0;
                for (int j = i + 1; j < selectedDates.size(); j++) {
                    int futureLessonIdx = selectedDates.get(j).getDayIndex() - 1;
                    if (t.getLessons()[futureLessonIdx] == null) {
                        futureAvail++;
                    }
                }
                t.setFutureAvail(futureAvail);
                t.setTodayRandom(rand.nextDouble());
            }

            available.sort((a, b) -> {
                int countA = teacherCounts.get(a.getName()).get("total");
                int countB = teacherCounts.get(b.getName()).get("total");
                if (countA != countB) {
                    return Integer.compare(countA, countB);
                }
                if (a.getFutureAvail() != b.getFutureAvail()) {
                    return Integer.compare(a.getFutureAvail(), b.getFutureAvail());
                }
                return Double.compare(a.getTodayRandom(), b.getTodayRandom());
            });

            // 상위 N명 추출
            Teacher[] selectedTeachers = new Teacher[n];
            for (int k = 0; k < n; k++) {
                if (k < available.size()) {
                    selectedTeachers[k] = available.get(k);
                } else {
                    selectedTeachers[k] = null;
                }
            }

            // 장소 균등 최적화 함수 호출
            String[] assignedPlaces = getBestLocationAssignment(selectedTeachers, rand, n);

            // 통계 업데이트
            for (int k = 0; k < n; k++) {
                String name = assignedPlaces[k];
                if (!name.equals("인원부족")) {
                    Map<String, Integer> counts = teacherCounts.get(name);
                    counts.put("total", counts.get("total") + 1);
                    counts.put(places.get(k), counts.get(places.get(k)) + 1);
                }
            }

            results.add(new Assignment(dateObj.getDate(), dateObj.getDayOfWeek(), assignedPlaces));
        }

        return results;
    }

    private String[] getBestLocationAssignment(Teacher[] selectedTeachers, Random rand, int n) {
        List<int[]> perms = new ArrayList<>();
        generatePermutations(new int[n], 0, new boolean[n], perms);

        int[] bestPerm = perms.get(0);
        double minScore = Double.MAX_VALUE;

        for (int[] perm : perms) {
            double score = 0;
            for (int i = 0; i < n; i++) {
                Teacher teacher = selectedTeachers[perm[i]];
                if (teacher != null) {
                    String locName = places.get(i);
                    int locCount = teacherCounts.get(teacher.getName()).get(locName);
                    score += (locCount * locCount);
                }
            }

            double randomJitter = rand.nextDouble() * 0.1;
            if (score + randomJitter < minScore) {
                minScore = score + randomJitter;
                bestPerm = perm;
            }
        }

        String[] assignedNames = new String[n];
        for (int i = 0; i < n; i++) {
            Teacher targetTeacher = selectedTeachers[bestPerm[i]];
            assignedNames[i] = (targetTeacher != null) ? targetTeacher.getName() : "인원부족";
        }
        return assignedNames;
    }

    private void generatePermutations(int[] arr, int depth, boolean[] visited, List<int[]> result) {
        if (depth == arr.length) {
            result.add(arr.clone());
            return;
        }
        for (int i = 0; i < arr.length; i++) {
            if (!visited[i]) {
                visited[i] = true;
                arr[depth] = i;
                generatePermutations(arr, depth + 1, visited, result);
                visited[i] = false;
            }
        }
    }
}
