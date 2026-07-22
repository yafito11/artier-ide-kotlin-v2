class FallbackMiddleware {
    constructor() {
        this.providers = [];
        this.providerStatus = new Map();
        this.loadProviders();
    }

    loadProviders() {
        const OpenAIProvider = require('../providers/openai');
        const ClaudeProvider = require('../providers/claude');
        const GeminiProvider = require('../providers/gemini');

        this.providers = [
            new OpenAIProvider(),
            new ClaudeProvider(),
            new GeminiProvider()
        ].sort((a, b) => a.priority - b.priority);

        // Initialize status
        this.providers.forEach(provider => {
            this.providerStatus.set(provider.name, {
                available: provider.isAvailable(),
                lastUsed: null,
                errorCount: 0
            });
        });
    }

    getProvider(model) {
        // Find provider that supports the model
        for (const provider of this.providers) {
            if (provider.getModels().includes(model) && provider.isAvailable()) {
                return provider;
            }
        }

        // Return first available provider as fallback
        return this.providers.find(p => p.isAvailable());
    }

    getAvailableModels() {
        const models = [];
        this.providers.forEach(provider => {
            if (provider.isAvailable()) {
                provider.getModels().forEach(model => {
                    models.push({
                        id: model,
                        object: 'model',
                        owned_by: provider.name,
                        permission: []
                    });
                });
            }
        });
        return models;
    }

    getProviderStatus() {
        const status = [];
        this.providers.forEach(provider => {
            const providerStatus = this.providerStatus.get(provider.name);
            status.push({
                name: provider.name,
                available: provider.isAvailable(),
                models: provider.getModels(),
                priority: provider.priority,
                lastUsed: providerStatus?.lastUsed,
                errorCount: providerStatus?.errorCount
            });
        });
        return status;
    }

    async handleFailure(request) {
        const { model } = request;
        
        // Try other providers
        for (const provider of this.providers) {
            if (provider.getModels().includes(model) && provider.isAvailable()) {
                try {
                    const response = await this.makeRequest(provider, request);
                    return response;
                } catch (error) {
                    console.error(`Provider ${provider.name} failed:`, error.message);
                    this.incrementErrorCount(provider.name);
                }
            }
        }

        throw new Error('All providers failed');
    }

    async makeRequest(provider, request) {
        const axios = require('axios');
        
        const headers = {
            'Content-Type': 'application/json',
            ...provider.getHeaders()
        };

        const providerRequest = provider.transformRequest(request);
        const response = await axios.post(provider.getEndpoint(), providerRequest, {
            headers,
            timeout: 60000
        });

        return provider.transformResponse(response.data);
    }

    incrementErrorCount(providerName) {
        const status = this.providerStatus.get(providerName);
        if (status) {
            status.errorCount++;
            this.providerStatus.set(providerName, status);
        }
    }

    updateLastUsed(providerName) {
        const status = this.providerStatus.get(providerName);
        if (status) {
            status.lastUsed = Date.now();
            this.providerStatus.set(providerName, status);
        }
    }
}

module.exports = FallbackMiddleware;