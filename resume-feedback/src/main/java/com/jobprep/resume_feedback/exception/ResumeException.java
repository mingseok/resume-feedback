package com.jobprep.resume_feedback.exception;

import com.jobprep.resume_feedback.common.exception.CommonError;
import com.jobprep.resume_feedback.common.exception.ServiceException;

public class ResumeException extends ServiceException {

    public ResumeException(CommonError commonError) {
        super(commonError);
    }
}
