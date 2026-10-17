package com.example.rta.controller;

import com.example.rta.response.ToTalInformation;
import com.example.rta.service.RtaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@CrossOrigin
@RequestMapping(path = "/api")
public class RtaController {

    @Autowired
    private RtaService rtaService;

    @ResponseBody // @ResponseBody注解将 java 对象转化为 json 返回给前端
    @RequestMapping(value = "/rta", method = {RequestMethod.GET, RequestMethod.POST})
    public ToTalInformation Rta(@RequestBody(required = false) String requestBody) {
        return this.rtaService.analyse(requestBody);
    }
}
