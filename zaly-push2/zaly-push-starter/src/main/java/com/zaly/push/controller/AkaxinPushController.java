package com.zaly.push.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/akaxin")
public class AkaxinPushController {

	@RequestMapping(method = RequestMethod.POST, value = "/push")
	@ResponseBody
	public String sendNotification(@RequestBody byte[] body) {
		return "success";
	}

}
