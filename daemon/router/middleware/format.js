class FormatMiddleware {
    constructor() {
        this.formats = {
            openai: 'openai',
            claude: 'claude',
            gemini: 'gemini'
        };
    }

    transformRequest(provider, request) {
        const providerName = provider.name;
        
        switch (providerName) {
            case 'openai':
                return this.toOpenAIFormat(request);
            case 'claude':
                return this.toClaudeFormat(request);
            case 'gemini':
                return this.toGeminiFormat(request);
            default:
                return request;
        }
    }

    transformResponse(provider, response) {
        const providerName = provider.name;
        
        switch (providerName) {
            case 'openai':
                return this.fromOpenAIFormat(response);
            case 'claude':
                return this.fromClaudeFormat(response);
            case 'gemini':
                return this.fromGeminiFormat(response);
            default:
                return response;
        }
    }

    toOpenAIFormat(request) {
        return {
            model: request.model,
            messages: request.messages,
            temperature: request.temperature || 0.7,
            max_tokens: request.max_tokens || 4096,
            stream: request.stream || false
        };
    }

    toClaudeFormat(request) {
        const systemMessage = request.messages.find(m => m.role === 'system');
        const otherMessages = request.messages.filter(m => m.role !== 'system');

        return {
            model: request.model,
            max_tokens: request.max_tokens || 4096,
            system: systemMessage ? systemMessage.content : undefined,
            messages: otherMessages.map(msg => ({
                role: msg.role === 'assistant' ? 'assistant' : 'user',
                content: msg.content
            }))
        };
    }

    toGeminiFormat(request) {
        const contents = request.messages.map(msg => ({
            role: msg.role === 'assistant' ? 'model' : 'user',
            parts: [{ text: msg.content }]
        }));

        return {
            contents,
            generationConfig: {
                temperature: request.temperature || 0.7,
                maxOutputTokens: request.max_tokens || 4096
            }
        };
    }

    fromOpenAIFormat(response) {
        return {
            id: response.id,
            object: 'chat.completion',
            created: response.created,
            model: response.model,
            choices: response.choices.map(choice => ({
                index: choice.index,
                message: choice.message,
                finish_reason: choice.finish_reason
            })),
            usage: response.usage
        };
    }

    fromClaudeFormat(response) {
        return {
            id: response.id || `chatcmpl-${Date.now()}`,
            object: 'chat.completion',
            created: Math.floor(Date.now() / 1000),
            model: response.model,
            choices: [{
                index: 0,
                message: {
                    role: 'assistant',
                    content: response.content[0].text
                },
                finish_reason: response.stop_reason === 'end_turn' ? 'stop' : response.stop_reason
            }],
            usage: {
                prompt_tokens: response.usage?.input_tokens || 0,
                completion_tokens: response.usage?.output_tokens || 0,
                total_tokens: (response.usage?.input_tokens || 0) + (response.usage?.output_tokens || 0)
            }
        };
    }

    fromGeminiFormat(response) {
        const candidate = response.candidates?.[0];
        const text = candidate?.content?.parts?.[0]?.text || '';

        return {
            id: `chatcmpl-${Date.now()}`,
            object: 'chat.completion',
            created: Math.floor(Date.now() / 1000),
            model: 'gemini-pro',
            choices: [{
                index: 0,
                message: {
                    role: 'assistant',
                    content: text
                },
                finish_reason: candidate?.finishReason || 'stop'
            }],
            usage: {
                prompt_tokens: response.usageMetadata?.promptTokenCount || 0,
                completion_tokens: response.usageMetadata?.candidatesTokenCount || 0,
                total_tokens: response.usageMetadata?.totalTokenCount || 0
            }
        };
    }

    // Token reduction (RTK) - Simplified
    reduceTokens(messages, maxTokens = 4096) {
        let totalTokens = 0;
        const reducedMessages = [];

        // Keep system message if present
        const systemMessage = messages.find(m => m.role === 'system');
        if (systemMessage) {
            reducedMessages.push(systemMessage);
            totalTokens += this.estimateTokens(systemMessage.content);
        }

        // Add messages from most recent, fitting within token limit
        const otherMessages = messages.filter(m => m.role !== 'system').reverse();
        for (const message of otherMessages) {
            const messageTokens = this.estimateTokens(message.content);
            if (totalTokens + messageTokens <= maxTokens) {
                reducedMessages.push(message);
                totalTokens += messageTokens;
            } else {
                break;
            }
        }

        return reducedMessages.reverse();
    }

    estimateTokens(text) {
        // Rough estimation: 1 token ≈ 4 characters
        return Math.ceil(text.length / 4);
    }
}

module.exports = FormatMiddleware;