import React from 'react';

interface Props {
  agentName: string;
  sessionId: string;
}

interface Message {
  role: 'user' | 'agent';
  text: string;
}

export default function AgentChat({ agentName, sessionId }: Props) {
  const [messages, setMessages] = React.useState<Message[]>([]);
  const [input, setInput] = React.useState('');
  const [ws, setWs] = React.useState<WebSocket | null>(null);

  React.useEffect(() => {
    if (!agentName || !sessionId) return;
    const socket = new WebSocket(
      `/run_live?app_name=${encodeURIComponent(agentName)}&session_id=${sessionId}`
    );
    socket.onmessage = (ev) => {
      const evt = JSON.parse(ev.data);
      if (evt.type === 'AGENT_MESSAGE') {
        setMessages((m) => [...m, { role: 'agent', text: evt.message.content.parts[0].text }]);
      }
    };
    setWs(socket);
    return () => socket.close();
  }, [agentName, sessionId]);

  const send = () => {
    if (!ws) return;
    ws.send(JSON.stringify({ content: { parts: [{ text: input }] } }));
    setMessages((m) => [...m, { role: 'user', text: input }]);
    setInput('');
  };

  return (
    <div>
      <div style={{ border: '1px solid #ccc', padding: '0.5rem', height: '200px', overflow: 'auto' }}>
        {messages.map((m, idx) => (
          <div key={idx}><b>{m.role}:</b> {m.text}</div>
        ))}
      </div>
      <input
        type="text"
        value={input}
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={(e) => e.key === 'Enter' && send()}
      />
      <button onClick={send}>Send</button>
    </div>
  );
}
