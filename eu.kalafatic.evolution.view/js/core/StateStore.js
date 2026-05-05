export class StateStore {
    constructor(initialState = {}, eventBus) {
        this.state = initialState;
        this.eventBus = eventBus;
        this.subscribers = [];
    }

    getState() {
        return this.state;
    }

    setState(newState) {
        const oldState = { ...this.state };
        this.state = { ...this.state, ...newState };
        this.notify(this.state, oldState);
    }

    subscribe(callback) {
        this.subscribers.push(callback);
        callback(this.state);
        return () => {
            this.subscribers = this.subscribers.filter(sub => sub !== callback);
        };
    }

    notify(state, oldState) {
        this.subscribers.forEach(callback => callback(state, oldState));
        if (this.eventBus) {
            this.eventBus.emit('state:changed', { state, oldState });
        }
    }
}

// Initial system state
export const initialState = {
    messages: [],
    changes: [],
    isSidePanelOpen: true,
    feedbackLevel: 'full', // 'simple', 'interactive', 'advanced', 'full'
    isThinking: false,
    lastDiffs: {}, // path -> diff
    currentContextFile: null
};
