package com.haris.MechanicApp.Repository;

import com.haris.MechanicApp.Model.Questions.Questions;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Questions ,Integer> {

    public List<Questions>  findByParentQuestion(Questions parentQuestion);

   List<Questions>  findByParentQuestionIdIsNull();
}
