package com.printwithai.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(MaxUploadSizeExceededException exc, RedirectAttributes redirectAttributes) {
        log.warn("Uploaded file exceeds maximum limit: {}", exc.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", "The file is too large. Maximum allowed size is 10 MB.");
        return "redirect:/print/new";
    }

    @ExceptionHandler(InvalidFileException.class)
    public String handleInvalidFileException(InvalidFileException exc, RedirectAttributes redirectAttributes) {
        log.warn("Invalid file uploaded: {}", exc.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", exc.getMessage());
        return "redirect:/print/new";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException exc, Model model) {
        log.warn("Requested resource not found: {}", exc.getMessage());
        model.addAttribute("errorMessage", exc.getMessage());
        return "error/404";
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public String handleInvalidOrderState(InvalidOrderStateException exc, Model model) {
        log.warn("Order state error: {}", exc.getMessage());
        model.addAttribute("errorMessage", exc.getMessage());
        return "error/500";
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public String handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException exc, Model model) {
        log.warn("Resource or path not found: {}", exc.getMessage());
        model.addAttribute("errorMessage", "The requested page was not found.");
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception exc, Model model) {
        log.error("Unhandled exception: ", exc);
        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
        return "error/500";
    }
}
