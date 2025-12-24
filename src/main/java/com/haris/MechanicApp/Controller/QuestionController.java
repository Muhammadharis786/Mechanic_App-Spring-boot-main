package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class QuestionController {


    @Autowired
    private QuestionService qservice;




@GetMapping("api/problems/root")
public ResponseEntity <?> getByRoot (){
    return qservice.findbysubproblems(null);

}

    @GetMapping("api/problem/subproblems/{parentID}")

    public ResponseEntity<?> getsubproblems(@PathVariable Integer parentID) {
        return  qservice.findbysubproblems(parentID);

    }



}


