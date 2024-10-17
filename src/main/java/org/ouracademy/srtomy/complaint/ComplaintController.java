package org.ouracademy.srtomy.complaint;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("complaints")
@AllArgsConstructor
@Tag(name = "complaints")
public class ComplaintController {

    ComplaintRepository complaintRepository;
    
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public Complaint create(@RequestBody @Valid Complaint complaint) {
        return complaintRepository.save(complaint);
    }
}
