package com.example.algamoneyapi.resource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.algamoneyapi.event.RecursoCriadoEvent;
import com.example.algamoneyapi.exceptionhandler.AlgamoneyExecptionHandler.Erro;
import com.example.algamoneyapi.model.Lancamento;
import com.example.algamoneyapi.repository.LancamentoRepository;
import com.example.algamoneyapi.repository.filter.LancamentoFilter;
import com.example.algamoneyapi.repository.projection.ResumoLancamento;
import com.example.algamoneyapi.service.LancamentoService;
import com.example.algamoneyapi.service.exception.PessoaInexistenteOuInativaException;

@RestController
@RequestMapping("/lancamento")
@CrossOrigin("https://projeto-algamoney-ui.herokuapp.com/")
public class LancamentoResource {
	
	@Autowired
	private LancamentoRepository lancamentoRepository;
	
	@Autowired
	private LancamentoService lancamentoService;
	
	@Autowired
	private ApplicationEventPublisher publisher;
	
	@Autowired
	private MessageSource messageSource;
	
		
	@GetMapping
	public Page<Lancamento> pesquisar(LancamentoFilter lancamentoFilter, Pageable pageable){
		return lancamentoRepository.filtrar(lancamentoFilter, pageable);
	}
	
	@GetMapping(params = "resumo")
	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and #oauth2.hasScope('read')") // libera autorizacao via BD + Scope para o usuario
	public Page<ResumoLancamento> resumir(LancamentoFilter lancamentoFilter, Pageable pageable){
		return lancamentoRepository.resumir(lancamentoFilter, pageable);
	}
	
	@GetMapping("/{codigo}")
	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and #oauth2.hasScope('read')") // libera autorizacao via BD + Scope para o usuario
	public ResponseEntity<Lancamento> buscarPeloCodigo(@PathVariable Long codigo){
		Optional<Lancamento> lancamento = this.lancamentoRepository.findById(codigo);
		return lancamento.isPresent() ? ResponseEntity.ok(lancamento.get()):
			ResponseEntity.notFound().build();
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('ROLE_CADASTRAR_LANCAMENTO') and #oauth2.hasScope('write')") // libera autorizacao via BD + Scope para o usuario
	public ResponseEntity<Lancamento> criarLancamento(@Valid @RequestBody Lancamento lancamento, HttpServletResponse response){
		Lancamento lancamentoSalvo = lancamentoService.salvar(lancamento);
		publisher.publishEvent(new RecursoCriadoEvent(this, response, lancamentoSalvo.getCodigo()));
		return ResponseEntity.status(HttpStatus.CREATED).body(lancamentoSalvo);
	}
	
	@ExceptionHandler({ PessoaInexistenteOuInativaException.class})
	public ResponseEntity<Object> handlePessoaInexistenteOuInativaException(PessoaInexistenteOuInativaException ex){
		String mensagemUsuario = messageSource.getMessage("pessoa.inativa-ou-inexistente", null, LocaleContextHolder.getLocale());
		String mensagemDesenvolvedor = ex.toString();
		List<Erro> erros = Arrays.asList(new Erro (mensagemUsuario, mensagemDesenvolvedor));
		return ResponseEntity.badRequest().body(erros);
	}
	
	@DeleteMapping("/{codigo}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('ROLE_REMOVER_LANCAMENTO') and #oauth2.hasScope('write')") // libera autorizacao via BD + Scope para o usuario
	public void remover(@PathVariable long codigo) {
		lancamentoRepository.deleteById(codigo);
	}
	
	
	//metodo para realizar alterar lancamento de cadastro
	@PutMapping("/{codigo}")
	@PreAuthorize("hasAuthority('ROLE_CADASTRAR_LANCAMENTO')  and #oauth2.hasScope('read')")
	public ResponseEntity<Lancamento> atualizar(@PathVariable Long codigo, @Valid @RequestBody Lancamento lancamento) {
		
		try {
			Lancamento lancamentoSalvo = lancamentoService.atualizar(codigo, lancamento);
			return ResponseEntity.ok(lancamentoSalvo);
			
		} catch (Exception e) {
			return ResponseEntity.notFound().build();
		}
		
	}
	
	
}
