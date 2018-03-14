<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page isELIgnored="false" %>
<%--
  Created by IntelliJ IDEA.
  User: jiaqi
  Date: 2018/1/17
  Time: 上午10:59
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <%
        String path = request.getContextPath();
        String basePath = request.getScheme() + "://"
                + request.getServerName() + ":" + request.getServerPort()
                + path + "/";
    %>
    <base href="<%=basePath%>" />

    <meta charset="UTF-8">
    <title>管理员登录</title>
    <script src="resources/js/jquery-3.1.1.min.js" type="text/javascript"></script>
    <link href="resources/css/login.css" rel="stylesheet">
    <link href="resources/css/flat-ui.min.css" rel="stylesheet">
    <link href="resources/css/vendor/bootstrap/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
<div>
    <form action="checkPass" method="POST">
        <div id="loginDiv" class="loginContent">
            <h3 class="headline">管理员登录</h3>
            <div class="control-group">
                <span class="questionSpan">密码：</span>
                <input type="password" class="login-field" value="" placeholder="Password" id="login-pass" name="pass">
                <%--<button onclick="checkPass();" id="btnSubmit" class="btn btn-hg btn-primary btn-wide">提交</button>--%>
                <button id="btnSubmit" onclick="checkPass();" class="btn btn-hg btn-primary btn-wide">提交</button>
                <span class="info">${loginInfo}</span>
            </div>
        </div>
    </form>
</div>
</body>
</html>
