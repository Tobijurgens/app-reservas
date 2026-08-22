import { Link } from 'react-router-dom';

const NegocioCard = ({ negocio, onEliminar, onEditar }) => {
  return (
    <article className="border border-gray-200 rounded-xl p-5 bg-white hover:shadow-md transition-shadow flex flex-col justify-between h-full">
      <header className="flex justify-between items-start mb-2">
        <h3 className="text-lg font-bold text-gray-900">{negocio.nombre}</h3>
        <button 
          onClick={() => onEliminar(negocio.id, negocio.nombre)}
          className="text-gray-400 hover:text-red-500 transition-colors p-1"
          aria-label={`Eliminar ${negocio.nombre}`}
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
        </button>
      </header>
      
      <p className="text-sm text-gray-500 mb-4 break-all">
        agendapp.com/negocio/<span className="text-blue-600 font-semibold">{negocio.alias}</span>
      </p>
      <p className="text-gray-700 text-sm mb-4 flex-1">{negocio.descripcion}</p>

      <footer className="mt-auto flex flex-col gap-2">
        <div className="flex flex-col sm:flex-row gap-2">
          <Link 
            to={`/dashboard/negocio/${negocio.id}`} 
            className="flex-1 bg-gray-50 hover:bg-gray-100 border border-gray-200 text-gray-700 text-center py-2 rounded-lg text-sm font-medium transition-colors"
          >
            Administrar
          </Link>
          <button 
            onClick={() => onEditar(negocio)} 
            className="flex-1 bg-blue-50 hover:bg-blue-100 text-blue-700 py-2 rounded-lg text-sm font-medium transition-colors"
          >
            Editar Info
          </button>
        </div>
        <a 
          href={`/negocio/${negocio.alias}`} 
          target="_blank" 
          rel="noopener noreferrer"
          className="w-full bg-white border border-blue-200 text-blue-600 hover:bg-blue-50 text-center py-2 rounded-lg text-sm font-medium transition-colors flex items-center justify-center gap-1"
        >
          Ver perfil público
        </a>
      </footer>
    </article>
  );
};

export default NegocioCard;