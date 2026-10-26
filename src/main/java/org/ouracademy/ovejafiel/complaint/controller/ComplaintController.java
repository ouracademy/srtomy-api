package org.ouracademy.ovejafiel.complaint.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.complaint.command.create_complaint.CreateComplaintCommand;
import org.ouracademy.ovejafiel.complaint.command.create_complaint.CreateComplaintCommandHandler;
import org.ouracademy.ovejafiel.complaint.controller.dto.ComplaintResponse;
import org.ouracademy.ovejafiel.complaint.controller.dto.CreateComplaintRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("complaints")
@AllArgsConstructor
@Tag(name = "complaints")
public class ComplaintController {

  CreateComplaintCommandHandler createComplaintCommandHandler;

  @PostMapping
  @ResponseStatus(code = HttpStatus.CREATED)
  public ComplaintResponse create(@RequestBody @Valid CreateComplaintRequest request) {
    return ComplaintResponse.from(
        createComplaintCommandHandler.handle(
            new CreateComplaintCommand(
                request.firstName(),
                request.lastName(),
                request.dni(),
                request.address(),
                request.email(),
                request.comment())));
  }
}
