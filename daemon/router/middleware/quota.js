class QuotaMiddleware {
    constructor() {
        this.quotas = new Map();
        this.usage = new Map();
        this.loadDefaultQuotas();
    }

    loadDefaultQuotas() {
        // Default quotas per model (tokens per day)
        const defaultQuotas = {
            'gpt-4': { daily: 100000, monthly: 2000000 },
            'gpt-4-turbo': { daily: 200000, monthly: 4000000 },
            'gpt-3.5-turbo': { daily: 1000000, monthly: 20000000 },
            'claude-3-opus': { daily: 100000, monthly: 2000000 },
            'claude-3-sonnet': { daily: 200000, monthly: 4000000 },
            'claude-3-haiku': { daily: 1000000, monthly: 20000000 },
            'gemini-pro': { daily: 200000, monthly: 4000000 },
            'gemini-1.5-pro': { daily: 100000, monthly: 2000000 },
            'gemini-1.5-flash': { daily: 1000000, monthly: 20000000 }
        };

        Object.entries(defaultQuotas).forEach(([model, quota]) => {
            this.quotas.set(model, quota);
            this.usage.set(model, {
                daily: 0,
                monthly: 0,
                lastReset: Date.now()
            });
        });
    }

    checkQuota(model) {
        const quota = this.quotas.get(model);
        const usage = this.usage.get(model);

        if (!quota || !usage) {
            return { allowed: true };
        }

        // Reset daily quota if needed
        const now = Date.now();
        const dayMs = 24 * 60 * 60 * 1000;
        if (now - usage.lastReset > dayMs) {
            usage.daily = 0;
            usage.lastReset = now;
        }

        // Check limits
        if (usage.daily >= quota.daily) {
            return {
                allowed: false,
                message: `Daily quota exceeded for ${model}. Limit: ${quota.daily} tokens`
            };
        }

        if (usage.monthly >= quota.monthly) {
            return {
                allowed: false,
                message: `Monthly quota exceeded for ${model}. Limit: ${quota.monthly} tokens`
            };
        }

        return { allowed: true };
    }

    updateUsage(model, usage) {
        const currentUsage = this.usage.get(model);
        if (currentUsage) {
            currentUsage.daily += usage.total_tokens || 0;
            currentUsage.monthly += usage.total_tokens || 0;
            this.usage.set(model, currentUsage);
        }
    }

    getQuotaStatus() {
        const status = {};
        
        this.quotas.forEach((quota, model) => {
            const usage = this.usage.get(model);
            status[model] = {
                daily: {
                    used: usage?.daily || 0,
                    limit: quota.daily,
                    remaining: quota.daily - (usage?.daily || 0)
                },
                monthly: {
                    used: usage?.monthly || 0,
                    limit: quota.monthly,
                    remaining: quota.monthly - (usage?.monthly || 0)
                }
            };
        });

        return status;
    }

    setQuota(model, daily, monthly) {
        this.quotas.set(model, { daily, monthly });
    }

    resetQuota(model) {
        this.usage.set(model, {
            daily: 0,
            monthly: 0,
            lastReset: Date.now()
        });
    }
}

module.exports = QuotaMiddleware;