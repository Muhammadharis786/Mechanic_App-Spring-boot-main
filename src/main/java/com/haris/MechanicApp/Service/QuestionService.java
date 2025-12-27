package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.Questions.Questions;
import com.haris.MechanicApp.Repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository QuestionRepo;


     public ResponseEntity<?> findbysubproblems(Integer parentID) {


    Optional<Questions> questions = QuestionRepo.findById(parentID);

        if(questions.isPresent()){
            Questions question = questions.get();
            List<Questions>  subproblems =  QuestionRepo.findByParentQuestion(question);
            return ResponseEntity.ok(subproblems);
        }
        return ResponseEntity.notFound().build();




    }



}
