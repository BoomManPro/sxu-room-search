package cn.boommanpro.sxu.crawler.parse;





import cn.boommanpro.sxu.common.StringUtils;
import cn.boommanpro.sxu.crawler.dto.XxXqPageFruit;
import cn.boommanpro.sxu.crawler.model.ListWeekStruct;
import cn.boommanpro.sxu.util.DateUtil;
import cn.boommanpro.sxu.model.ClassRoomPage;
import lombok.extern.log4j.Log4j2;
import me.ghui.fruit.Fruit;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class JsoupMethod {
    private Document document = null;
    private Elements links = null;
    private String Data = null;
    private String Value = null;
    private String result;

    public JsoupMethod(String result) {
        document = Jsoup.parse(result);
        this.result=result;
    }

    /**
     * @return 获得学年学期和校区
     */
    public  Map<String, String> getXxxq() {

        XxXqPageFruit xxXqPageFruit = new Fruit().fromHtml(result, XxXqPageFruit.class);
        Map<String, String> xxxqMap = new HashMap<>();
        for (XxXqPageFruit.XxXq xxXq: xxXqPageFruit.getXxXqList()) {
            if (StringUtils.notBlank(xxXq.getName(),xxXq.getValue())){
                xxxqMap.put(xxXq.getValue(), xxXq.getName());
            }
        }
        return xxxqMap;
    }
    /**
     * @return 获得学年学期和校区
     */
    public String getXnxq() {
        JSONArray XNXQ = new JSONArray();
        links = document.select("option");
        // 从Html中找出 option 标签
        // Data 用来存放数据
        for (Element link : links) {

            Data = link.text();
            if (Data.contains("学年")) {
                /*
                 * 用来检测jsoup查找错误
                 * */
//                 System.out.println("学年学期:" + Data + "\t value : " +links.get(i).attr("value"));
                XNXQ.add(link.attr("value"));
            }
        }

        return  XNXQ.get(0).toString();
    }

    public JSONObject Get_List_Data(String SchoolZone, String BuildingValue) {
        JSONObject List_Data = new JSONObject();
        JSONObject Result = new JSONObject();
        links = document.select("script");
        Data = links.toString();
        Data = Data.substring(Data.indexOf("<option"), Data.indexOf("</select>"));
        document = Jsoup.parse(Data);
        links = document.select("option");
        for (int i = 0; i < links.size(); i++) {
            Data = links.get(i).text();
            Value = links.get(i).attr("value");
            if (!Data.equals("") && !Data.contains("无信息")) {
                // System.out.println("教学楼:" + Data + "\t value : " + Value);
                List_Data.put(Data, Value);
            }
        }
        if (BuildingValue != null) {
            Result.put(BuildingValue, List_Data);
        } else {
            Result.put(SchoolZone, List_Data);
        }
        // System.out.println(Result);
        return Result;

    }

    List<ListWeekStruct.Week> ListWeek;

    public Map<String, String> Get_Post_Data() {

// TODO: 2018/5/2 课表清空可能不一样


        links = document.select("table");
        int TableSize = links.size();
        ListWeekStruct WeekStruct = new ListWeekStruct();
        ListWeek = WeekStruct.Get_ListWeek();

        /*
         * Size==1 无课表 无活动安排 Size==3 只有活动安排 Size==4 只有课表 Size==6 两者都有
         *
         */
        ClassRoomPage classRoomPage = new Fruit().fromHtml(result, ClassRoomPage.class);
        if (classRoomPage.getTable()!=null){
            for (int i = 0,tableSize=classRoomPage.table.size(); i < tableSize; i++) {
                ClassRoomPage.Table table=classRoomPage.table.get(i);
                int columnNumber = table.getColumnNumber();
                switch (columnNumber){
                    case 1:break;
                    case 13:GetWeekData(i,11);break;
                    case 6:GetWeekData(i,0);break;
                    default:
                        if (i!=0){
                            log.info(columnNumber);
                            log.info("需要查看是否需要重新设计");
                            log.info(result);
                        }
                         break;
                }
            }
        }

//        switch (TableSize) {
//            case 0:
//                return null;
//            case 1:
//                return null;
//            case 3:
//                GetWeekData(2, 0);// 第一个参数为个数 不一定只有活动安排
//                break;
//            case 4:
//                GetWeekData(3, 11);// 课表情况
//                break;
//            case 6:
//                GetWeekData(3, 11);
//                GetWeekData(5, 0);
//                break;
//                default:break;
//        }
        return Get_List_Week_Data();

    }

    private Map<String, String> Get_List_Week_Data() {
        //不需要CourseName和TeacherName
        Map<String, String> treeMap = null;
        for (int m = 0; m < ListWeek.size(); m++) {
            for (int i = 1; i <= 7; i++) {
                // TODO: 2018/1/25 计算时间
                //计算时间后放入json中
                JSONObject contentJson = new JSONObject();
                boolean flag = false;
                String date = null;
                for (int j = 1; j <= 10; j++) {
                    if (ListWeek.get(m).GetDay(i).GetCourse(j).isStatue()) {
                        // TODO: 2018/1/25 判断
//                        System.out.println("第" + (m + 1) + "周\t星期" + i + "\t第" + j + "节 课程情况");
//                        System.out.println("课程名称:" + ListWeek.get(m).GetDay(i).GetCourse(j).getCourse() + "\t老师:"
//                                + ListWeek.get(m).GetDay(i).GetCourse(j).getTeacher());
                        flag = true;
                        date = DateUtil.weekNumberToDateStr(m + 1, i);
//                        contentJson.put(j, new DayCourse(ListWeek.get(m).GetDay(i).GetCourse(j).getCourse(), ListWeek.get(m).GetDay(i).GetCourse(j).getTeacher()));
                        JSONObject courseTeacher = new JSONObject();
                        courseTeacher.put(ListWeek.get(m).GetDay(i).GetCourse(j).getCourse(),ListWeek.get(m).GetDay(i).GetCourse(j).getTeacher());
                        contentJson.put(j, courseTeacher);

                    }
                    // TODO: 2018/1/25 整合功能
                }
                if (flag) {
                    if (treeMap == null)
                        treeMap = new TreeMap<>();
                    treeMap.put(date, contentJson.toString());
                }

            }
        }
        return treeMap;
    }

    private void GetWeekData(int n, int m) {
        // 创建存储信息的List
        Elements Class_tr = null;
        Elements Class_td = null;
        String WeekTime = null;
        String WeekPart = null;
        String Course = null;
        String Teacher = null;
        Class_tr = links.get(n).select("tr");
        for (int i = 1; i < Class_tr.size(); i++) {
            // TODO: 2018/4/17 获取部分非动态 写死的不符合很多学校
            Class_td = Class_tr.get(i).select("td");
            WeekTime = Class_td.get(m).text();
            WeekPart = Class_td.get(m + 1).text();
            if (m == 0) {
                if (!Class_td.get(2).text().equals("")) {
                    Course = Class_td.get(2).text();
                    Teacher = Class_td.get(3).text();
                }
            } else if (m == 11) {
                if (!Class_td.get(0).text().equals("")) {
                    Course = Class_td.get(0).text();
                    Teacher = Class_td.get(7).text();
                }
            }
//			System.out.println(Course + "\t" + Teacher + "\t" + WeekTime + "\t" + WeekPart);
            SetDatatiWeekList(GetWeekTime(WeekTime), GetWeekPart(WeekPart), Course, Teacher);
        }

    }

    private void SetDatatiWeekList(List<Integer> TimeList, List<String> PartList, String Course, String Teacher) {
        boolean Time[] = TimeListToArrayInt(TimeList, PartList);


        for (int i = 1; i < ListWeek.size() + 1; i++) {
            if (Time[i]) {
                ListWeek.get(i - 1).SetData(PartList.get(0), PartList.get(1), Course, Teacher);
                if (!PartList.get(2).equals("")) {
                    ListWeek.get(i - 1).SetData(PartList.get(0), PartList.get(2), Course, Teacher);
                }
            }
        }

    }


    private List<Integer> GetWeekTime(String WeekTime) {
        /*
         * 14-14.---最多5对 1-4,6-13 分为first and sencond second not belive ，not add
         * and stop
         */
        // WeekTime="1-4,6-13,1-4,6-13,1-4,6-13";

        List<Integer> TimeList = new ArrayList<>();

        String Regex = "(?<first1>\\d{1,2})\\-{0,1}(?<sencond1>\\d{0,2}),{0,1}(?<first2>\\d{0,2})\\-{0,1}(?<sencond2>\\d{0,2}),{0,1}(?<first3>\\d{0,2})\\-{0,1}(?<sencond3>\\d{0,2}),{0,1}(?<first4>\\d{0,2})\\-{0,1}(?<sencond4>\\d{0,2}),{0,1}(?<first5>\\d{0,2})\\-{0,1}(?<sencond5>\\d{0,2}),{0,1}(?<first6>\\d{0,2})\\-{0,1}(?<sencond6>\\d{0,2})";
        Pattern pattern = Pattern.compile(Regex);
        Matcher matcher = pattern.matcher(WeekTime);
        matcher.find();
        boolean AddStatue = true;
        String First = null;
        String Second = null;
        for (int i = 1; i <= 6 && AddStatue; i++) {
            First = matcher.group("first" + i);
            Second = matcher.group("sencond" + i);
            if (!First.equals("")) {
                TimeList.add(Integer.parseInt(First));
                if (!Second.equals("")) {
                    TimeList.add(Integer.parseInt(Second));
                } else {
                    TimeList.add(0);
                }
            } else {
                AddStatue = false;
            }
        }

        /*
         * 测试
         */

        // for(int i=0;i<TimeList.size();i++){
        // System.out.println(TimeList.get(i).intValue());
        // }

        return TimeList;
    }

    private boolean[] TimeListToArrayInt(List<Integer> TimeList, List<String> PartList) {
        boolean Time[] = new boolean[30];
        int PartSize = PartList.size();

        int First = 0;
        int Second = 0;
        int TimeSize = TimeList.size();
        String Single = null;
        if (PartSize != 3) {
            for (int n = 0; n < TimeSize; n += 2) {
                First = TimeList.get(n);
                Second = TimeList.get(n + 1);

                if (Second != 0) {
                    for (int i = First; i <= Second; i++) {
                        Time[i] = true;
                    }
                } else {
                    Time[First] = true;
                }
            }
        } else {
            Single = PartList.get(2);
            for (int n = 0; n < TimeSize; n += 2) {
                First = TimeList.get(n);
                Second = TimeList.get(n + 1);
                if (Second != 0) {
                    for (int i = First; i <= Second; i++) {
                        if (isSingle(i, Single) != 0)
                            Time[i] = true;
                    }
                } else {
                    Time[First] = true;
                }
            }
        }

        /*
         * 测试
         */

        // for(int i=1;i<Time.length;i++){
        // if(Time[i])
        // System.out.println("This is"+i+"周" +"有课");
        // else{
        // System.out.println("This is"+i+"周" +"无课");
        // }
        // }
        return Time;
    }

    private List<String> GetWeekPart(String WeekPart) {
        // WeekPart 五[1-2节]单
//        System.out.println(WeekPart);
        List<String> PartList = new ArrayList<>();
        /*
         * PartList if have three now this is juidge is Single if 1-2返回一 if3-4
         * 返回二 if 5-6 返回三 以此类推
         */
        String WeekDay = null;
        String PartNumfirst = null;
        String PartNumsecond = null;
        String Single = null;
        String Regex = "(?<WeekDay>[\u4e00-\u9fa5])" + "\\[" + "(?<PartNumfirst>\\d{0,2})"
                + "\\-{0,1}(?<PartNumsecond>\\d{0,2})" + "[\u4e00-\u9fa5]\\]{0,1}" + "(?<Single>[\u4e00-\u9fa5]{0,1})";
        Pattern pattern = Pattern.compile(Regex);
        Matcher matcher = pattern.matcher(WeekPart);
        matcher.find();
        WeekDay = matcher.group("WeekDay");
        PartNumfirst = matcher.group("PartNumfirst");
        PartNumsecond = matcher.group("PartNumsecond");
        Single = matcher.group("Single");

        PartList.add(WeekDay);
        PartList.add(PartNumfirst);
        PartList.add(PartNumsecond);
        PartList.add(Single);

        /*
         * 测试
         */
        // for(int i=0;i<PartList.size();i++){
        // System.out.println(PartList.get(i));
        // //如果没有 为""
        // }
        return PartList;

    }

    private int isSingle(int WeekTime, String Single) {
        switch (Single) {
            case "单":
                if (WeekTime % 2 == 1)
                    return WeekTime;
                else
                    return 0;
            case "双":
                if (WeekTime % 2 == 0)
                    return WeekTime;
                else
                    return 0;
            default:
                return 0;
        }

    }
}
