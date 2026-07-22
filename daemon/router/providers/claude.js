class ClaudeProvider {
    constructor(config = {}) {
        this.name = 'claude';
        this.apiKey = config.apiKey || process.env.ANTHROPIC_API_KEY;
        this.baseUrl = config.baseUrl || 'https://api.anthropic.com';
        this.models = ['claude-3-opus', 'claude-3-sonnet', 'claude-3-haiku'];
        this.priority = 2;
    }

    getEndpoint() {
        return `${this.baseUrl}/v1/messages`;
    }

    getHeaders() {
        return {
            'x-api-key': this.apiKey,
            'anthropic-version': '2023-06-01',
            'content-type': 'application/json'
        };
    }

    getModels() {
        return this.models;
    }

    isAvailable() {
        return !!this.apiKey;
    }

    transformRequest(request) {
        // Transform OpenAI format to Claude format
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

    transformResponse(response) {
        // Transform Claude format to OpenAI format
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

    calculateCost(usage) {
        // Pricing per 1K tokens (approximate)
        const pricing = {
            'claude-3-opus': { input: 0.015, output: 0.075 },
            'claude-3-sonnet': { input: 0.003, output: 0.015 },
            'claude-3-haiku': { input: 0.00025, output: 0.00125 }
        };

        const modelPricing = pricing[request.model] || pricing['claude-3-sonnet'];
        const inputCost = (usage.prompt_tokens || 0) / 1000 * modelPricing.input;
        const outputCost = (usage.completion_tokens || 0) / 1000 * modelPricing.output;

        return inputCost + outputCost;
    }
}

module.exports = ClaudeProvider;