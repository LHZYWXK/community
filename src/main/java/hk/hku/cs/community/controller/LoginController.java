package hk.hku.cs.community.controller;

import com.google.code.kaptcha.Producer;
import hk.hku.cs.community.entity.User;
import hk.hku.cs.community.service.UserService;
import hk.hku.cs.community.utils.CommunityConstant;
import hk.hku.cs.community.utils.CommunityUtils;
import hk.hku.cs.community.utils.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

@Controller
public class LoginController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private MailClient mailClient;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg",
                    "??????????????????????????????????????????????????????????????????????????????");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    // http://localhost:8080/community/activation/{userId}/{activationCode}
    @RequestMapping(path = "/activation/{userId}/{activationCode}", method = RequestMethod.GET)
    public String activation(Model model,
                             @PathVariable("userId") int userId,
                             @PathVariable("activationCode") String activationCode) {
        int result = userService.activation(userId, activationCode);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "????????????????????????????????????????????????");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "????????????????????????????????????");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "????????????????????????????????????????????????");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session) {
        // ???????????????
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // ?????????????????? session
        session.setAttribute("kaptcha", text);

        // ???????????????????????????
        response.setContentType("image/png");
        try {
            ServletOutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("????????????????????????" + e.getMessage());
        }
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String activationCode, boolean rememberMe,
                        Model model, HttpSession session, HttpServletResponse response) {
        // ???????????????
        String kaptcha = (String) session.getAttribute("kaptcha");
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(activationCode)
                || !kaptcha.equalsIgnoreCase(activationCode)) {
            model.addAttribute("codeMsg", "?????????????????????");
            return "/site/login";
        }

        // ?????????????????????
        int expiredSeconds = rememberMe ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        return "redirect:/login";
    }

    // ??????????????????
    @RequestMapping(path = "/forget", method = RequestMethod.GET)
    public String getForgetPage() {
        return "/site/forget";
    }

    // ???????????????
    @RequestMapping(path = "/forget/code", method = RequestMethod.GET)
    @ResponseBody
    public String getForgetCode(String email, HttpSession session) {
        if (StringUtils.isBlank(email)) {
            return CommunityUtils.getJSONString(1, "?????????????????????");
        }

        // ????????????
        Context context = new Context();
        context.setVariable("email", email);
        String code = CommunityUtils.generateUUID().substring(0, 4);
        context.setVariable("verifyCode", code);
        String content = templateEngine.process("/mail/forget", context);
        mailClient.sendMail(email, "????????????", content);

        // ???????????????
        session.setAttribute("verifyCode", code);

        return CommunityUtils.getJSONString(0);
    }

    // ????????????
    @RequestMapping(path = "/forget/password", method = RequestMethod.POST)
    public String resetPassword(String email, String verifyCode, String password, Model model, HttpSession session) {
        String code = (String) session.getAttribute("verifyCode");
        if (StringUtils.isBlank(verifyCode) || StringUtils.isBlank(code) || !code.equalsIgnoreCase(verifyCode)) {
            model.addAttribute("codeMsg", "???????????????!");
            return "/site/forget";
        }

        Map<String, Object> map = userService.resetPassword(email, password);
        if (map.containsKey("user")) {
            return "redirect:/login";
        } else {
            model.addAttribute("emailMsg", map.get("emailMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/forget";
        }
    }
}
