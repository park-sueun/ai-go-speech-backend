package com.aigo.speech.ai.client;

import com.aigo.speech.ai.dto.AiPromptRequest;
import com.aigo.speech.ai.dto.AiResponse;

public interface AiProvider {

	String getProvider();

	int getPriority();

	boolean isEnabled();

	String complete(String systemPrompt, String userPrompt);

	AiResponse complete(AiPromptRequest request, String renderedPrompt);
}
