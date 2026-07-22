class OpenAIProvider {
    constructor(config = {}) {
        this.name = 'openai';
        this.apiKey = config.apiKey || process.env.OPENAI_API_KEY;
        this.baseUrl = config.baseUrl || 'https://api.openai.com';
        this.models = ['gpt-4', 'gpt-4-turbo', 'gpt-3.5-turbo'];
        this.priority = 1;
    }

    getEndpoint() {
        return `${this.baseUrl}/v1/chat/completions`;
    }

    getHeaders() {
        return {
            'Authorization': `Bearer ${this.apiKey}`
        };
    }

    getModels() {
        return this.models;
    }

    isAvailable() {
        return !!this.apiKey;
    }

    transformRequest(request) {
        return {
            model: request.model,
            messages: request.messages,
            temperature: request.temperature || 0.7,
            max_tokens: request.max_tokens || 4096,
            stream: request.stream || false
        };
    }

    transformResponse(response) {
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

    calculateCost(usage) {
        // Pricing per 1K tokens (approximate)
        const pricing = {
            'gpt-4': { input: 0.03, output: 0.06 },
            'gpt-4-turbo': { input: 0.01, output: 0.03 },
            'gpt-3.5-turbo': { input: 0.001, output: 0.002 }
        };

        const modelPricing = pricing[request.model] || pricing['gpt-3.5-turbo'];
        const inputCost = (usage.prompt_tokens || 0) / 1000 * modelPricing.input;
        const outputCost = (usage.completion_tokens || 0) / 1000 * modelPricing.output;

        return inputCost + outputCost;
    }
}

module.exports = OpenAIProvider;