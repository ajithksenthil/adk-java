import React from 'react';
import AgentUploader from './components/AgentUploader';
import AgentList from './components/AgentList';
import AgentChat from './components/AgentChat';

export default function App() {
  const [selectedAgent, setSelectedAgent] = React.useState<string | null>(null);
  const [sessionId, setSessionId] = React.useState<string>('');

  return (
    <div style={{ padding: '1rem', fontFamily: 'sans-serif' }}>
      <h1>ADK Web Scripts</h1>
      <AgentUploader />
      <hr />
      <AgentList onSelect={setSelectedAgent} />
      {selectedAgent && (
        <div>
          <h2>Chat with {selectedAgent}</h2>
          <input
            type="text"
            placeholder="session id"
            value={sessionId}
            onChange={(e) => setSessionId(e.target.value)}
          />
          <AgentChat agentName={selectedAgent} sessionId={sessionId} />
        </div>
      )}
    </div>
  );
}
