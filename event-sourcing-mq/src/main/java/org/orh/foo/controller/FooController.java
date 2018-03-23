package org.orh.foo.controller;

import org.orh.foo.service.FooService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FooController {
    private FooService fooService;

    @Autowired
    public FooController(FooService fooService) {
        this.fooService = fooService;
    }

    @GetMapping("/foo/{name}")
    public void foo(@PathVariable("name") String name) {
        fooService.insertFoo(name);
    }
}
