export interface SseMessagePayload<T = any> {
    id: number;
    status: string;
    message: string;
    data?: T;
    executionTime?: number;
}

export interface FileMessage {
    id: number;
    status: string;
    name: string;
}