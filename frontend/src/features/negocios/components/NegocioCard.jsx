import { Link } from 'react-router-dom';

const NegocioCard = ({ negocio, onEliminar, onEditar }) => {
  return (
    <article className="border border-gray-100 rounded-2xl p-6 bg-white hover:shadow-lg transition-all flex flex-col justify-between h-full group">
      <header className="flex justify-between items-start mb-2">
        <h3 className="text-lg font-bold text-app-black transition-colors">{negocio.nombre}</h3>
        <button 
          onClick={() => onEliminar(negocio.id, negocio.nombre)}
          className="text-gray-300 hover:text-app-red transition-colors p-1"
          aria-label={`Eliminar ${negocio.nombre}`}
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
        </button>
      </header>
      
      <p className="text-sm font-semibold text-app-marineblue mb-4 break-all">
        agendapp.com/negocio/<span>{negocio.alias}</span>
      </p>
      <p className="text-app-black text-sm mb-6 flex-1 line-clamp-3">{negocio.descripcion}</p>

      <footer className="mt-auto flex flex-col gap-3">
        <div className="flex flex-col sm:flex-row gap-3">
          <Link 
            to={`/dashboard/negocio/${negocio.id}`} 
            className="flex-1 bg-app-marineblue hover:bg-app-black/90 text-white text-center py-2.5 rounded-xl text-sm font-medium transition-colors shadow-sm duration-300"
          >
            Administrar
          </Link>
          <button 
            onClick={() => onEditar(negocio)} 
            className="flex-1 bg-app-white hover:bg-app-black/20 text-app-marineblue py-2.5 rounded-xl text-sm font-medium transition-colors shadow-sm duration-300"
          >
            Editar Info
          </button>
        </div>
        <a 
          href={`/negocio/${negocio.alias}`} 
          target="_blank" 
          rel="noopener noreferrer"
          className="w-full bg-app-white border-app-white text-app-marineblue  hover:bg-app-black/20 text-center py-2.5 rounded-xl text-sm font-medium transition-all shadow-sm flex items-center justify-center gap-2 duration-300"
        >
          Ver perfil público
        </a>
      </footer>
    </article>
  );
};

export default NegocioCard;