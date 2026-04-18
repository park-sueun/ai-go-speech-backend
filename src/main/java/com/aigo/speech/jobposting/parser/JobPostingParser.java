package com.aigo.speech.jobposting.parser;

import com.aigo.speech.jobposting.dto.JobPostingAnalyzeResponse;

public interface JobPostingParser {
	JobPostingAnalyzeResponse parse (String rawText);
}
