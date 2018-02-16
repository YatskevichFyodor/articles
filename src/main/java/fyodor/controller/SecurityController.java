package fyodor.controller;

import fyodor.model.UserLoginDto;
import fyodor.model.UserRegistrationDto;
import fyodor.registration.OnRegistrationCompleteEvent;
import fyodor.model.User;
import fyodor.service.IUserService;
import fyodor.service.SecurityService;
import fyodor.validation.UserLoginValidator;
import fyodor.validation.UserRegistrationValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

@Controller
public class SecurityController {

    @Autowired
    private IUserService userService;

    @Autowired
    private UserRegistrationValidator userRegistrationValidator;

    @Autowired
    private UserLoginValidator userLoginValidator;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    LocaleResolver localeRsolver;

    @Autowired
    SecurityService securityService;

    @RequestMapping(value = {"/login"})
    public String login(Model model) {
        model.addAttribute("userLoginDto", new UserLoginDto());
        return "login";
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String loginPost(@ModelAttribute("userLoginDto") UserLoginDto userLoginDto, Errors errors, HttpServletRequest request, Model model) {
        userLoginValidator.validate((Object) userLoginDto, errors);
        if (errors.hasErrors())
            return "login";
        securityService.autologin(userLoginDto.getUsername(), userLoginDto.getPassword());
        return "redirect:/home";
    }

    @RequestMapping(value = {"/logout"})
    public String logout() {
        SecurityContextHolder.getContext().setAuthentication(null);
        return "redirect:/login";
    }

    @RequestMapping(value = "/reg")
    public String registration(Model model) {
        model.addAttribute("userRegistrationDto", new UserRegistrationDto());
        return "reg";
    }

    @RequestMapping(value = "/reg", method = RequestMethod.POST)
    public String registration(@ModelAttribute("userRegistrationDto") UserRegistrationDto userRegistrationDto, Errors errors, HttpServletRequest request, Model model) {
        userRegistrationValidator.validate((Object) userRegistrationDto, errors);
        if (errors.hasErrors())
            return new String("reg");
        User user;
        String appUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        user = userService.save(userRegistrationDto);

        eventPublisher.publishEvent(new OnRegistrationCompleteEvent(user, localeRsolver.resolveLocale(request), appUrl));

        String message = messageSource.getMessage("registration.message.confirm", null, localeRsolver.resolveLocale(request));
        model.addAttribute("message", message);
        return "index";
    }

    @RequestMapping(value = "/registrationconfirm", method = RequestMethod.GET)
    public String confirmRegistration(@RequestParam("username") String user, @RequestParam("hash") String hash, HttpServletRequest request, Model model) {
        String message;
        boolean confermed = userService.confirm(user, hash);
        Locale locale = localeRsolver.resolveLocale(request);
        if (confermed) {
            message = messageSource.getMessage("auth.message.successful", null, locale);
            model.addAttribute("message", message);
//            model.addAttribute("message", "auth.successful");
            return "index";
        }

        message = messageSource.getMessage("auth.message.fail", null, locale);
        model.addAttribute("message", message);
//        model.addAttribute("message", "auth.expired");
        return "index";
    }

}
