class GeminiProvider {
    constructor(config = {}) {
        this.name = 'gemini';
        this.apiKey = config.apiKey || process.env.GOOGLE_API_KEY;
        this.baseUrl = config.baseUrl || 'https://generativelanguage.googleapis.com';
        this.models = ['gemini-pro', 'gemini-1.5-pro', 'gemini-1.5-flash'];
        this.priority = 3;
    }

    getEndpoint() {
        return `${this.baseUrl}/v1beta/models/${this.models[0]}:generateContent?key=${this.apiKey}`;
    }

    getHeaders() {
        return {
            'Content-Type': 'application/json'
        };
    }

    getModels() {
        return this.models;
    }

    isAvailable() {
        return !!this.apiKey;
    }

    transformRequest(request) {
        // Transform OpenAI format to Gemini format
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

    transformResponse(response) {
        // Transform Gemini format to OpenAI format
        const candidate = response.candidates?.[0];
        const text = candidate?.content?.parts?.[0]?.text || '';

        return {
            id: `chatcmpl-${Date.now()}`,
            object: 'chat.completion',
            created: Math.floor(Date.now() / 1000),
            model: this.models[0],
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

    calculateCost(usage) {
        // Gemini pricing is complex, simplified here
        const pricing = {
            'gemini-pro': { input: 0.00025, output: 0.0005 },
            'gemini-1.5-pro': { input: 0.00125, output: 0.005 },
            'gemini-1.5-flash': { input: 0.000075, output: 0.0003 }
        };

        const modelPricing = pricing[request.model] || pricing['gemini-pro'];
        const inputCost = (usage.prompt_tokens || 0) / 1000 * modelPricing.input;
        const outputCost = (usage.completion_tokens || 0) / 1000 * modelPricing.output;

        return inputCost + outputCost;
    }
}

module.exports = GeminiProvider;