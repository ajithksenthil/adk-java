import React from 'react';

interface Props {
  onSelect: (name: string) => void;
}

export default function AgentList({ onSelect }: Props) {
  const [agents, setAgents] = React.useState<string[]>([]);

  React.useEffect(() => {
    fetch('/agents')
      .then((res) => res.json())
      .then((data) => setAgents(data.agents ?? []))
      .catch(() => setAgents([]));
  }, []);

  return (
    <div>
      <h2>Available Agents</h2>
      <ul>
        {agents.map((name) => (
          <li key={name}>
            <button onClick={() => onSelect(name)}>{name}</button>
          </li>
        ))}
      </ul>
    </div>
  );
}
