package com.zhanliao.controller;

import com.zhanliao.erro.BusinessException;
import com.zhanliao.erro.EmBusinessError;
import com.zhanliao.response.CommonReturnType;
import com.zhanliao.service.OrderService;
import com.zhanliao.service.model.OrderModel;
import com.zhanliao.service.model.UserModel;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author: ZhanLiao
 * @Description:
 * @Date: 2021/4/8 10:45
 * @Version: 1.0
 */
@Controller("order")
@RequestMapping("/order")
//@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
//@CrossOrigin(allowCredentials = "true", origins = "http://127.0.0.1:8848")
@CrossOrigin(originPatterns = "*",allowCredentials="true")    // 2.24版本以上是不能在设定allowCredentials = "true“时候，设定allowedHeaders = "*"，将解决跨域访问的问题
//@CrossOrigin
public class OrderController extends BaseController {
    @Autowired
    OrderService orderService;

    @Autowired
    HttpServletRequest httpServletRequest;

    // 封装请求下单
    @RequestMapping(value = "/createOrder", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED}) //后面这个参数需要和前端对应,
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name = "itemId")Integer itemId, @RequestParam(name = "promoId", required = false)Integer promoId, @RequestParam(name = "amount")Integer amount) throws BusinessException {
        // 获取用户的登录信息
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if (isLogin == null || !isLogin.booleanValue()) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户还未登录,不能下单");
        }
        UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");

        OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId, amount);//userModel.getId()
        return CommonReturnType.create(null);
    }
}
