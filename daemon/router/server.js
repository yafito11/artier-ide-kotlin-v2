const express = require('express');
const cors = require('cors');
const { createProxyMiddleware } = require('http-proxy-middleware');
const FallbackMiddleware = require('./middleware/fallback');
const QuotaMiddleware = require('./middleware/quota');
const FormatMiddleware = require('./middleware/format');

const app = express();
const PORT = process.env.ROUTER_PORT || 20128;

// Middleware — CORS only for loopback (native WebView / local tools)
app.use(cors({
    origin: (origin, cb) => {
        if (!origin || /^https?:\/\/(127\.0\.0\.1|localhost)(:\d+)?$/.test(origin)) {
            cb(null, true);
            return;
        }
        cb(new Error('CORS blocked'));
    }
}));
app.use(express.json());

// Initialize middleware
const fallback = new FallbackMiddleware();
const quota = new QuotaMiddleware();
const format = new FormatMiddleware();

// Request logging
app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
    next();
});

// Health check
app.get('/health', (req, res) => {
    res.json({ status: 'ok', timestamp: Date.now() });
});

// Models list
app.get('/v1/models', (req, res) => {
    const models = fallback.getAvailableModels();
    res.json({ data: models });
});

// Chat completions (main endpoint)
app.post('/v1/chat/completions', async (req, res) => {
    try {
        const { model, messages, stream = false } = req.body;
        
        // Check quota
        const quotaCheck = quota.checkQuota(model);
        if (!quotaCheck.allowed) {
            return res.status(429).json({
                error: {
                    message: quotaCheck.message,
                    type: 'quota_exceeded'
                }
            });
        }

        // Get provider for model
        const provider = fallback.getProvider(model);
        if (!provider) {
            return res.status(400).json({
                error: {
                    message: `No provider available for model: ${model}`,
                    type: 'invalid_model'
                }
            });
        }

        // Transform request to provider format
        const providerRequest = format.transformRequest(provider, req.body);

        // Make request to provider
        const response = await makeProviderRequest(provider, providerRequest);

        // Transform response to OpenAI format
        const openaiResponse = format.transformResponse(provider, response);

        // Update quota
        quota.updateUsage(model, response.usage || {});

        // Stream or normal response
        if (stream) {
            res.setHeader('Content-Type', 'text/event-stream');
            res.setHeader('Cache-Control', 'no-cache');
            res.setHeader('Connection', 'keep-alive');
            
            // Stream response
            streamProviderResponse(res, openaiResponse);
        } else {
            res.json(openaiResponse);
        }
    } catch (error) {
        console.error('Chat completion error:', error);
        
        // Try fallback
        try {
            const fallbackResponse = await fallback.handleFailure(req.body);
            res.json(fallbackResponse);
        } catch (fallbackError) {
            res.status(500).json({
                error: {
                    message: 'All providers failed',
                    type: 'provider_error'
                }
            });
        }
    }
});

// Quota status
app.get('/api/quota', (req, res) => {
    const quotaStatus = quota.getQuotaStatus();
    res.json(quotaStatus);
});

// Provider status
app.get('/api/providers', (req, res) => {
    const providers = fallback.getProviderStatus();
    res.json(providers);
});

// Configuration
app.get('/api/config', (req, res) => {
    const config = {
        port: PORT,
        providers: fallback.getProviderStatus(),
        quota: quota.getQuotaStatus()
    };
    res.json(config);
});

// Helper function to make provider request
async function makeProviderRequest(provider, request) {
    const axios = require('axios');
    
    const headers = {
        'Content-Type': 'application/json',
        ...provider.getHeaders()
    };

    const response = await axios.post(provider.getEndpoint(), request, {
        headers,
        timeout: 60000
    });

    return response.data;
}

// Helper function to stream response
function streamProviderResponse(res, response) {
    // Simulate streaming
    const chunks = JSON.stringify(response).match(/.{1,100}/g) || [];
    
    chunks.forEach((chunk, index) => {
        setTimeout(() => {
            res.write(`data: ${chunk}\n\n`);
            if (index === chunks.length - 1) {
                res.write('data: [DONE]\n\n');
                res.end();
            }
        }, index * 10);
    });
}

// Start server — bind loopback only (PRD §9 security)
const HOST = process.env.ROUTER_HOST || '127.0.0.1';
app.listen(PORT, HOST, () => {
    console.log(`9Router running on ${HOST}:${PORT}`);
    console.log(`Dashboard: http://${HOST}:${PORT}`);
    console.log(`API Endpoint: http://${HOST}:${PORT}/v1`);
});

module.exports = app;