package web.quiz.controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

import web.quiz.service.DBService;
import web.quiz.model.*;
import web.quiz.service.ResultService;

@Controller
public class QuizController {
    @Resource
    private DBService dbService;

    @Resource
    private ResultService resultService;

    @Value("${pass}")
    private String pass;
    @Value("${ftlTemplatePath}")
    private String ftlTemplatePath;
    @Value("${wordFolderPath}")
    private String wordFolderPath;
    @Value("${maxOptionNum}")
    private int maxOptionNum;

    private List<Question> questions;
    private List<Person> persons;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String defaultPage() {
        return "index";
    }

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/vote", method = RequestMethod.GET)
    public ModelAndView vote(HttpServletRequest request, ModelMap model) {
        //一个IP只能投一次
        String ip = request.getRemoteAddr();
        if (dbService.containsIP(ip)){
            return new ModelAndView("voteFailure");
        }


        List<String> names = new ArrayList<String>();
        List<String> ids = new ArrayList<String>();
        for (Person p : this.persons) {
            names.add(p.getName());
            ids.add(p.getId());
        }

        int questionNum = this.questions.size();
        String options[][] = new String[questionNum][this.maxOptionNum];
        String questionTitles[] = new String[questionNum];
        for (int i=0; i<questionNum; i++) {
            options[i] = this.questions.get(i).getOptions().split("#");
            questionTitles[i] = this.questions.get(i).getTitle();
        }

        model.addAttribute("names", names);
        model.addAttribute("ids", ids);
        model.addAttribute("titles", questionTitles);
        model.addAttribute("options", options);

        return new ModelAndView("vote");
    }

    @RequestMapping(value = "/fileDownload", method = RequestMethod.POST)
    public ResponseEntity<byte[]> fileDownload(HttpServletRequest request) throws Exception{

        //读取答案，统计结果
        System.out.println("分析结果…");
        List<Result> results = dbService.loadResults();
        if (results == null || results.isEmpty()){
            System.out.println("结果为空！");
            return null;
        }

        //是否过滤选票
        String checkValidate = request.getParameter("validate");
        //CheckBox的值，如果关闭的话为null，开启的话为on。
        boolean validate = false;
        if (checkValidate==null){
            System.out.println("不开启过滤");
        }
        else {
            validate = true;
            System.out.println("开启过滤");
        }

        //保存结果为Word
        resultService.printWord(this.persons, results, this.maxOptionNum, this.ftlTemplatePath, this.wordFolderPath, validate);
        System.out.println("saveToWord Success:" + this.wordFolderPath);

        //压缩Word文件夹
        String zipFilePath = this.wordFolderPath;
        if (!zipFilePath.endsWith("/")){
           zipFilePath += '/' ;
        }

        if (validate){
            zipFilePath += "results(filtrated).zip";
        }
        else{
            zipFilePath += "results.zip";
        }
        resultService.createZip(wordFolderPath, zipFilePath);
        System.out.println("createZip Success:" + zipFilePath);

        //提供下载zip文件
        File file;
        file = new File(zipFilePath);
        InputStream is = new FileInputStream(file);
        byte[] body = new byte[is.available()];
        int bytes = is.read(body);
        System.out.println("读取字节数：" + bytes);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment;filename=" + file.getName());
        //ContentType用于定义用户的浏览器或相关设备如何显示将要加载的数据，octet-stream代表任意的二进制数据）
        //不加这一句用Safari下载时出现.html后缀
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpStatus statusCode = HttpStatus.OK;
        return new ResponseEntity<byte[]>(body, headers, statusCode);

        //public ResponseEntity（T  body，
        //                       MultiValueMap < String，String > headers，
        //                       HttpStatus  statusCode）
        //HttpEntity使用给定的正文，标题和状态代码创建一个新的。
        //参数：
        //body - 实体机构
        //headers - 实体头
        //statusCode - 状态码
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView login(ModelMap model) {
        model.addAttribute("loginInfo", "");
        return new ModelAndView("login");
    }

    @RequestMapping(value = "/checkPass", method = RequestMethod.POST)
    public ModelAndView check(HttpServletRequest request, ModelMap model) {
        String pw = request.getParameter("pass");
        //不能是==，字符串对比用equals
        if (pw.equals(this.pass)) {
            System.out.println("match");
            model.addAttribute("persons", persons);
            return new ModelAndView("result");
        } else {
            model.addAttribute("loginInfo", "密码错误！");
            return new ModelAndView("login");
        }
    }

    @RequestMapping(value = "/resetIPs", method = RequestMethod.POST)
    @ResponseBody
    public String resetIPs(){
        dbService.resetIP();
        System.out.println("resetIP success");
        return "resetIP success";
    }

    @RequestMapping(value = "/getVoterNum", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getVoterNum(){
        Map<String, Object> map = new HashMap<String, Object>();
        List<Result> results = dbService.loadResults();
        map.put("totalVoterNum", results.size());
        map.put("currentVoterNum", dbService.numOfNewIPs());
        return map;
    }

    //在方法的参数列表中添加形参 ModelMap map,spring 会自动创建ModelMap对象。
    //然后调用map的put(key,value)或者addAttribute(key,value)将数据放入map中，spring会自动将数据存入request。
    //单个人的成绩
    @RequestMapping(value = "detail", method = RequestMethod.POST)
    public ModelAndView detail(HttpServletRequest request, ModelMap model) {
        String id = request.getParameter("id");
        String name = request.getParameter("name");
        //是否过滤选票
        String checkValidate = request.getParameter("validate");
        //CheckBox的值，如果关闭的话为null，开启的话为on。
        boolean validate = false;
        if (checkValidate==null || checkValidate.equals("false")){
            System.out.println("不开启过滤");
        }
        else {
            validate = true;
            System.out.println("开启过滤");
        }

        System.out.println("分析答案…");
        List<Result> results = dbService.loadResults();
        int [][][] counts = resultService.doStatistics(results, this.maxOptionNum, validate);
        if (counts == null){
            return new ModelAndView("empty");
        }
        int questionNum = this.questions.size();
        int [][] count = counts[Integer.parseInt(id)-1];
        String [][] options = new String[questionNum][this.maxOptionNum];
        for (int i=0; i<questionNum; i++) {
            options[i] = this.questions.get(i).getOptions().split("#");
        }
        model.addAttribute("name", name);
        model.addAttribute("questions", this.questions);
        model.addAttribute("options", options);
        model.addAttribute("counts", count);
        System.out.println("分析完毕");
        return new ModelAndView("detail");
    }

    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    public String submit(HttpServletRequest request) {
        //一个IP只能投一次
        String ip = request.getRemoteAddr();
        if (dbService.containsIP(ip)){
            return "voteFailure";
        }

        //StringBuffer为字符串变量，拼接字符串时效率较高
        StringBuilder stringBuffer = new StringBuilder("");
        //获得表单中所有值
        Enumeration<String> enu = request.getParameterNames();
        while (enu.hasMoreElements()) {
            //每趟循环读取一个Person的成绩
            //读取questionNum个答案
            for (int i = 0; i < this.questions.size(); i++) {
                String paraName = enu.nextElement();
                stringBuffer.append(request.getParameter(paraName));
            }
            //每组得分末尾加#以示区分,如，person1得分#person2得分
            stringBuffer.append('#');
        }

        String scoreStr = stringBuffer.toString();
        //生成新的Result对象
        Result result = new Result();
        //记录IP
        result.setIp(ip);
        result.setScoreStr(scoreStr);
        result.setValidate(resultService.checkValidVote(scoreStr));
        //将成绩写入数据库中
        dbService.saveOrUpdateResult(result);

        //返回result界面,显示结果
        return "voteSuccess";
    }

    @PostConstruct
    private void initQuiz(){
        //从数据库读取信息
        System.out.println("读取答卷……");
        this.questions = dbService.loadQuestions();
        System.out.println("读取完毕");

        System.out.println("读取人员……");
        this.persons = dbService.loadPersons();
        System.out.println("读取完毕");

        System.out.println("人数:" + persons.size());
        System.out.println("题目数：" + questions.size());

        //每次重新启动网站时清空IP
        dbService.resetIP();
    }
}