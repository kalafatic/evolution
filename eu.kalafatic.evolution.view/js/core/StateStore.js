class StateStore {
    constructor(initialState = {}, eventBus = null) {
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
        this.notify(oldState);
    }

    subscribe(callback) {
        this.subscribers.push(callback);
        return () => {
            this.subscribers = this.subscribers.filter(sub => sub !== callback);
        };
    }

    notify(oldState) {
        this.subscribers.forEach(callback => callback(this.state, oldState));
        if (this.eventBus) {
            this.eventBus.emit('state:changed', { state: this.state, oldState });
        }
    }
}

export default StateStore;
